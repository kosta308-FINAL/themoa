import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const getCalendarEvents = ({ startDate, endDate }) =>
  axiosInstance
    .get("/api/calendar/events", {
      params: { startDate, endDate },
    })
    .then(responseData);

export const createCalendarSchedule = (payload) =>
  axiosInstance.post("/api/calendar/schedules", payload).then(responseData);

export const updateCalendarSchedule = (scheduleId, payload) =>
  axiosInstance
    .patch(`/api/calendar/schedules/${scheduleId}`, payload)
    .then(responseData);

export const deleteCalendarSchedule = (scheduleId) =>
  axiosInstance.delete(`/api/calendar/schedules/${scheduleId}`);
