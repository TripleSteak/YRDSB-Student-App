using System.Collections.Generic;
using System.Net.Sockets;

namespace YRDSBStudentPlanner_Server.Networking
{
    /// <summary>
    /// Represents a connection state corresponding to a single network user.
    /// </summary>
    public sealed class Connection
    {
        /// <summary>
        /// There should be exactly 1 socket per connection.
        /// </summary>
        public Socket Socket { get; set; }
        
        public const int BufferSize = 1024; // maximum message size per packet
        public byte[] Buffer = new byte[BufferSize]; // buffers incoming messages
        public List<byte> Message = new List<byte>(); // compiled message in bytes
    }
}
