using YRDSBStudentPlanner_Shared.Networking.Packet;

namespace YRDSBStudentPlanner_Server.Networking.EventArgs
{
    /// <summary>
    /// Event object related to packet/user interaction.
    /// </summary>
    public sealed class PacketEventArgs
    {
        public NetworkUser User { get; set; }
        public Packet Packet { get; set; }
    }
}
