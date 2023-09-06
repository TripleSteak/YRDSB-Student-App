using Google.Apis.Calendar.v3;
using Google.Apis.Calendar.v3.Data;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using YRDSBStudentPlanner_Shared;

namespace YRDSBStudentPlanner_Server.Data
{
    /// <summary>
    /// Each <see cref="School"/> has an associated Google Calendar.
    /// The <see cref="CalendarManager"/> loads each school's Google Calendar event data into a compressed string.
    /// </summary>
    public static class CalendarManager
    {
        /// <summary>
        /// Maps each <see cref="School"/> to a Google Calendar unique ID.
        /// </summary>
        public static readonly Dictionary<School, string> CalendarIDs = new Dictionary<School, string>
        {
            [School.BAYVIEW] = "{BAYVIEW CALENDAR ID}"
        };

        /// <summary>
        /// Represents each <see cref="School"/>'s Google Calendar data as a condensed string that can be sent to clients.
        /// </summary>
        public static readonly Dictionary<School, string> CalendarData = new Dictionary<School, string>();

        /// <summary>
        /// If kept true, will continue to periodically re-fetch Google Calendar event data.
        /// </summary>
        public static bool ShouldReloadCalendar;

        /// <summary>
        /// Begins loading Google Calendar data in its own thread.
        /// </summary>
        public static void LoadCalendars(CalendarService service)
        {
            ShouldReloadCalendar = true;

            var calRef = new ThreadStart(() => AsyncLoadCalendars(service));
            var calThread = new Thread(calRef);
            calThread.Start();
        }

        private static void AsyncLoadCalendars(CalendarService service)
        {
            while (ShouldReloadCalendar)
            {
                try
                {
                    Console.ForegroundColor = ConsoleColor.White;
                    Console.WriteLine("[CALENDAR] Loading calendar events...");

                    var curYear = DateTime.Now.Year;
                    var startTime = new DateTime(curYear - 1, 1, 1, 0, 0, 0); // Only load in events that are set to occur after this Date
                    var endTime = new DateTime(curYear + 3, 12, 31, 23, 59, 59); // Only load in events that are set to occur before this Date

                    foreach (var school in School.Schools)
                    {
                        var request = service.Events.List(CalendarIDs[school]);
                        request.TimeMin = startTime;
                        request.TimeMax = endTime;
                        request.MaxResults = 1250;
                        request.SingleEvents = true;
                        request.OrderBy = EventsResource.ListRequest.OrderByEnum.StartTime;

                        var events = request.Execute();
                        if (events.Items != null && events.Items.Count > 0)
                        {
                            // Load each event into a condensed data string
                            var calendarData = "";
                            var eventCounter = 0;

                            foreach (var eventItem in events.Items)
                            {
                                eventCounter++;

                                var summary = eventItem.Summary;
                                if (summary == null || summary.Equals("No Events", StringComparison.InvariantCultureIgnoreCase))
                                {
                                    continue;
                                }

                                var hasStartTime = true;
                                var hasEndTime = true;

                                var start = eventItem.Start.DateTime.ToString(); // YYYY-MM-DD H:MM:SS AM/PM
                                if (string.IsNullOrEmpty(start))
                                {
                                    // No time window exists
                                    start = eventItem.Start.Date;
                                    hasStartTime = false;
                                }

                                var end = eventItem.End.DateTime.ToString();
                                if (string.IsNullOrEmpty(end))
                                {
                                    end = eventItem.End.Date;
                                    hasEndTime = false;
                                }

                                var location = eventItem?.Location ?? "null";
                                calendarData += "|" + summary.Replace('|', ' ') + "|" + (hasStartTime ? "<&" : ">&") +
                                                start + "|" + (hasEndTime ? "<&" : ">&") + end + "|" +
                                                location.Replace('|', ' ') + "|##";
                            }

                            CalendarData[school] = calendarData;
                            Console.WriteLine("[CALENDAR] Successfully (re)loaded " + eventCounter +
                                              " calendar events for " + school.SchoolName);
                        }
                    }
                }
                catch (IOException ex)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine("[CALENDAR] Could not load calendar events.");
                    Console.ForegroundColor = ConsoleColor.White;
                    Console.WriteLine(ex.ToString());
                }

                try
                {
                    Thread.Sleep(3600000);
                }
                catch (Exception)
                {
                    // No action required
                }
            }
        }
    }
}