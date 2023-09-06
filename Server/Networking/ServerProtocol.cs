using System;
using YRDSBStudentPlanner_Server.Data;
using YRDSBStudentPlanner_Server.Networking.EventArgs;
using YRDSBStudentPlanner_Shared;
using YRDSBStudentPlanner_Shared.Networking;
using YRDSBStudentPlanner_Shared.Networking.Packet;

namespace YRDSBStudentPlanner_Server.Networking
{
    /// <summary>
    /// Handles how to respond to client-sent <see cref="Packet"/>s.
    /// </summary>
    public static class ServerProtocol
    {
        /// <summary>
        /// Parses the given <see cref="PacketEventArgs"/> and decides on an appropriate response.
        /// </summary>
        public static void ReadData(PacketEventArgs args)
        {
            var user = args.User;
            var packet = args.Packet;

            if (packet.Data is MessagePacketData)
            {
                var command = NetworkCommand.GetCommand(((MessagePacketData)packet.Data).Message);
                var details = NetworkCommand.GetInformation(((MessagePacketData)packet.Data).Message);

                if (command.Equals(NetworkCommand.School))
                {
                    Console.WriteLine("User " + user.Id + " has selected the school " + details);
                }
                else if (command.Equals(NetworkCommand.Calendar)) 
                {
                    // Send calendar data
                    Program.Listener.Send(user, NetworkCommand.Synthesize(NetworkCommand.Calendar, CalendarManager.CalendarData[School.GetFromName(details)]));
                }
                else if (command.Equals(NetworkCommand.Announcements)) 
                {
                    // Send announcements data
                    Program.Listener.Send(user, NetworkCommand.Synthesize(NetworkCommand.Announcements, TwitterManager.AnnouncementData[School.GetFromName(details)]));
                }
            }
        }
    }
}
