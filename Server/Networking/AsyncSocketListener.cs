using YRDSBStudentPlanner_Shared.Networking.Packet;
using YRDSBStudentPlanner_Server.Networking.EventArgs;
using System;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Threading;

namespace YRDSBStudentPlanner_Server.Networking
{
    /// <summary>
    /// TCP server socket endpoint that listens for messages on the network.
    /// </summary>
    public sealed class AsyncSocketListener
    {
        public ManualResetEvent _allDone = new ManualResetEvent(false);

        public event UserConnectedEvent OnUserConnected;
        public event UserDisconnectedEvent OnUserDisconnected;
        public event PacketReceivedEvent OnPacketReceived;
        public event PacketSentEvent OnPacketSent;

        /// <summary>
        /// Accumulator tracking quantity of network uploads.
        /// </summary>
        public long TotalBytesSent;
        
        /// <summary>
        /// Accumulator tracking quantity of network downloads.
        /// </summary>
        public long TotalBytesReceived;

        public const int Port = 8031;

        public AsyncSocketListener() { }

        /// <summary>
        /// Initializes the <see cref="Socket"/> listener and begins listening for connection attempts.
        /// </summary>
        public void StartListening()
        {
            var ip = IPAddress.Any;
            var localEndpoint = new IPEndPoint(ip, Port);
            var listener = new Socket(ip.AddressFamily, SocketType.Stream, ProtocolType.Tcp);

            try
            {
                listener.Bind(localEndpoint);
                listener.Listen(100);
                
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("YRDSB Student Planner listening server started on port {0}.", Port);
                Console.ForegroundColor = ConsoleColor.White;

                while(true)
                {
                    _allDone.Reset();
                    listener.BeginAccept(new AsyncCallback(AcceptCallback), listener);
                    _allDone.WaitOne();
                }
            } catch (Exception ex)
            {
                Console.WriteLine(ex.ToString());
            }
        }

        private void Send(NetworkUser user, Packet obj)
        {
            byte[] byteData = obj.Serialize();

            var socket = user.Connection.Socket;
            socket.BeginSend(byteData, 0, byteData.Length, SocketFlags.None, new AsyncCallback(SendCallback), new SendCallbackArgs { user = user, Packet = obj });
        }

        /// <summary>
        /// Attempts to send the given string data to the specified <see cref="NetworkUser"/>.
        /// </summary>
        public void Send(NetworkUser user, string data)
        {
            var packet = new Packet { Type = PacketType.Message, Data = new MessagePacketData(data) };
            Send(user, packet);
        }
        
        /// <summary>
        /// Invoked once we found a client trying to connect.
        /// </summary>
        private void AcceptCallback(IAsyncResult ar)
        {
            _allDone.Set();
            var listener = (Socket)ar.AsyncState;
            var handler = listener.EndAccept(ar);

            var user = new NetworkUser();
            user.Connection.Socket = handler;
            handler.BeginReceive(user.Connection.Buffer, 0, Connection.BufferSize, SocketFlags.None, new AsyncCallback(ReadCallback), user);
            OnUserConnected?.Invoke(this, new UserEventArgs(user));
        }

        /// <summary>
        /// Accepts and reads data sent from a client.
        /// </summary>
        private void ReadCallback(IAsyncResult ar)
        {
            var user = (NetworkUser)ar.AsyncState;
            var handler = user.Connection.Socket;

            var bytesRead = 0;
            try
            {
                bytesRead = handler.EndReceive(ar);
            } catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                OnUserDisconnected?.Invoke(this, new UserEventArgs(user));
                handler.Shutdown(SocketShutdown.Both);
                handler.Close();
            }

            TotalBytesReceived += bytesRead;

            if (bytesRead > 0)
            {
                user.Connection.Message.AddRange(user.Connection.Buffer.Take(bytesRead));

                var byteCount = BitConverter.ToInt32(user.Connection.Message.Take(sizeof(Int32)).ToArray(), 0);
                if (user.Connection.Message.Count == byteCount + sizeof(Int32))
                {
                    var p = Packet.Deserialize(user.Connection.Message);
                    OnPacketReceived?.Invoke(this, new PacketEventArgs { Packet = p, User = user });
                    user.Connection.Message.Clear();
                }
                handler.BeginReceive(user.Connection.Buffer, 0, Connection.BufferSize, SocketFlags.None, new AsyncCallback(ReadCallback), user);
            } else
            {
                OnUserDisconnected?.Invoke(this, new UserEventArgs(user));
                handler.Close();
            }
        }
        
        /// <summary>
        /// Callback method once <see cref="Send"/> has been invoked.
        /// </summary>
        private void SendCallback(IAsyncResult ar)
        {
            var args = (SendCallbackArgs)ar.AsyncState;

            var user = args.user;
            var socket = user.Connection.Socket;
            var bytesSent = 0;

            try
            {
                bytesSent = socket.EndSend(ar);
            } 
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                OnUserDisconnected?.Invoke(this, new UserEventArgs(user));
            }

            TotalBytesSent += bytesSent;
            OnPacketSent?.Invoke(this, new PacketEventArgs { User = user, Packet = args.Packet });
        }

        private class SendCallbackArgs
        {
            public NetworkUser user { get; internal set; }
            public Packet Packet { get; internal set; }
        }

        public delegate void UserConnectedEvent(object sender, UserEventArgs user);
        public delegate void UserDisconnectedEvent(object sender, UserEventArgs user);
        public delegate void PacketReceivedEvent(object sender, PacketEventArgs args);
        public delegate void PacketSentEvent(object sender, PacketEventArgs args);
    }
}
