namespace YRDSBStudentPlanner_Server.Networking
{
    /// <summary>
    /// Represents a single client connected to the server
    /// </summary>
    public class NetworkUser
    {
        /// <summary>
        /// Static variable that accumulates to assign a new ID to each new connection
        /// </summary>
        public static int IDSequence;
        
        /// <summary>
        /// Identifier for each client, which is unique among all past/present connections to this server instance.
        /// </summary>
        public int ID { get; set; }
        
        public int PacketNumber = 0;

        public Connection Connection { get; set; }

        public NetworkUser()
        {
            Connection = new Connection();
            ID = IDSequence++;
        }
    }
}
