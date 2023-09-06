namespace YRDSBStudentPlanner_Server.Networking.EventArgs
{
    /// <summary>
    /// Event object related to user interactions.
    /// </summary>
    public class UserEventArgs
    {
        public NetworkUser User { get; set; }

        public UserEventArgs(NetworkUser user) => User = user;
    }
}
