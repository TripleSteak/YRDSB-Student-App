using YRDSBStudentPlanner_Server.Networking;
using YRDSBStudentPlanner_Server.Networking.EventArgs;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Calendar.v3;
using Google.Apis.Auth.OAuth2;
using System.IO;
using Google.Apis.Util.Store;
using Google.Apis.Services;
using Google.Apis.Calendar.v3.Data;
using System.Diagnostics;
using YRDSBStudentPlanner_Server.Data;

namespace YRDSBStudentPlanner_Server
{
    public class Program
    {
        /// <summary>
        /// List of all active client connections.
        /// </summary>
        public static readonly List<NetworkUser> Users = new List<NetworkUser>();
        public static AsyncSocketListener Listener = new AsyncSocketListener();

        // Calendar API declarations
        private static string[] Scopes = { CalendarService.Scope.CalendarReadonly };
        private static string ApplicationName = "YRDSB Student Planner";

        public static void Main(string[] args)
        {
            Listener.OnUserConnected += (object sender, UserEventArgs e) =>
            {
                Console.WriteLine($"A user with ID {e.User.ID} has joined.");
                Users.Add(e.User);
            };

            Listener.OnUserDisconnected += (object sender, UserEventArgs e) =>
            {
                Console.WriteLine($"User with ID {e.User.ID} disconnected");
                Users.Remove(e.User);
            };

            Listener.OnPacketReceived += (object sender, PacketEventArgs e) => ServerProtocol.ReadData(e);

            long lastTotalReceived = 0;
            long lastTotalSent = 0;

            var stats = new System.Timers.Timer(1000);
            stats.Elapsed += (object sender, System.Timers.ElapsedEventArgs e) =>
            {
                var currentReceived = Listener.TotalBytesReceived - lastTotalReceived;
                lastTotalReceived = Listener.TotalBytesReceived;

                var currentSent = Listener.TotalBytesSent - lastTotalSent;
                lastTotalSent = Listener.TotalBytesSent;

                var kbSent = Listener.TotalBytesSent / 1000.0;
                var kbReceived = Listener.TotalBytesReceived / 1000.0;

                var kbSecSent = currentSent / 1000.0;
                var kbSecReceived = currentReceived / 1000.0;

                var statString = $"Send: {kbSent.ToString("N2")} kb ({kbSecSent.ToString("N2")} kb/s) | Receive: {kbReceived.ToString("N2")} kb ({kbSecReceived.ToString("N2")} kb/s)";
                Console.Title = ApplicationName + " Server | " + statString;
            };
            stats.Start();

            var listenRef = new ThreadStart(Listener.StartListening);
            var listenThread = new Thread(listenRef);
            listenThread.Start();

            // Initialize Google Calendar & Twitter API connections
            UserCredential credential;

            using (var stream = new FileStream("credentials.json", FileMode.Open, FileAccess.Read))
            {
                const string credPath = "token.json";
                credential = GoogleWebAuthorizationBroker.AuthorizeAsync(GoogleClientSecrets.Load(stream).Secrets, Scopes, "user", CancellationToken.None, new FileDataStore(credPath, true)).Result;
                Console.WriteLine("Credential file saved to: " + credPath);
            }

            var service = new CalendarService(new BaseClientService.Initializer()
            {
                HttpClientInitializer = credential,
                ApplicationName = ApplicationName
            });

            CalendarManager.LoadCalendars(service);
            TwitterManager.LoadAnnouncements();
        }
    }
}
