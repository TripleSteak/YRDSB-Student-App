using LinqToTwitter;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using YRDSBStudentPlanner_Shared;

namespace YRDSBStudentPlanner_Server.Data
{
    /// <summary>
    /// Fetches Twitter announcement data for various <see cref="School"/>s.
    /// </summary>
    public static class TwitterManager
    {
        /// <summary>
        /// Maps each <see cref="School"/> to the Twitter username that supplies its announcements.
        /// </summary>
        public static readonly Dictionary<School, List<string>> TwitterUsernames = new Dictionary<School, List<string>>
        {
            [School.BAYVIEW] = new[] { "{BAYVIEW TWITTER USERNAME #1}", "{BAYVIEW TWITTER USERNAME #2}" }.ToList()
        };

        /// <summary>
        /// Represents each <see cref="School"/>'s Twitter announcement data as a condensed string that can be sent to clients.
        /// </summary>
        public static readonly Dictionary<School, string> AnnouncementData = new Dictionary<School, string>();

        /// <summary>
        /// If kept true, will continue to periodically re-fetch Twitter announcements data
        /// </summary>
        public static bool ShouldReloadAnnouncements;

        private const string AccessToken = "{ACCESS TOKEN}";
        private const string AccessTokenSecret = "{ACCESS TOKEN SECRET}";
        private const string ConsumerKey = "{CONSUMER KEY}";
        private const string ConsumerSecret = "{CONSUMER SECRET}";

        /// <summary>
        /// Maximum number of tweets to load in for each Twitter announcements feed.
        /// </summary>
        private const int MaxTweets = 20;

        /// <summary>
        /// Begins loading Twitter announcements data in its own thread.
        /// </summary>
        public static void LoadAnnouncements()
        {
            ShouldReloadAnnouncements = true;

            var twitRef = new ThreadStart(AsyncLoadAnnouncements);
            var twitThread = new Thread(twitRef);
            twitThread.Start();
        }

        private static void AsyncLoadAnnouncements()
        {
            while (ShouldReloadAnnouncements)
            {
                var authorizer = new SingleUserAuthorizer
                {
                    CredentialStore = new InMemoryCredentialStore
                    {
                        ConsumerKey = ConsumerKey,
                        ConsumerSecret = ConsumerSecret,
                        OAuthToken = AccessToken,
                        OAuthTokenSecret = AccessTokenSecret
                    }
                };

                var twitterContext = new TwitterContext(authorizer);

                try
                {
                    foreach (var school in School.Schools)
                    {
                        var statusTweets = new List<Status>();
                        foreach (var str in TwitterUsernames[school])
                        {
                            var newList = from tweet in twitterContext.Status
                                where tweet.Type == StatusType.User && tweet.ScreenName == str &&
                                      tweet.Count == MaxTweets && tweet.IncludeEntities == false
                                select tweet;
                            statusTweets.AddRange(newList.ToList());
                        }

                        statusTweets.Sort(delegate(Status s1, Status s2)
                        {
                            return s2.CreatedAt.ToString().CompareTo(s1.CreatedAt.ToString());
                        });

                        var announcementsData = "";
                        var tweetCounter = 0;
                        foreach (var statusTweet in statusTweets)
                        {
                            tweetCounter++;
                            announcementsData += "|" + statusTweet.CreatedAt.ToString().Substring(0, 10) + "|" +
                                                 statusTweet.Text.Replace('|', ' ') + "|##";
                            if (tweetCounter >= MaxTweets) break;
                        }

                        AnnouncementData[school] = announcementsData;
                        Console.WriteLine(
                            "[TWITTER] Successfully (re)loaded {0} announcements for " + school.SchoolName,
                            tweetCounter);
                    }
                }
                catch (Exception ex)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine("[TWITTER] Could not load announcements.");
                    Console.ForegroundColor = ConsoleColor.White;
                    Console.WriteLine(ex.ToString());
                }

                try
                {
                    Thread.Sleep(600000);
                }
                catch (Exception)
                {
                    // No action required
                }
            }
        }
    }
}