/**
 * Independent time operation tool to facilitate subsequent switch to dayjs
 */
import dayjs from 'dayjs';

const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const DATE_FORMAT = 'YYYY-MM-DD';

export function formatToDateTime(
  date: dayjs.ConfigType | undefined = undefined,
  format = DATE_TIME_FORMAT,
): string {
  return dayjs(date).format(format);
}

export function formatToDate(
  date: dayjs.ConfigType | undefined = undefined,
  format = DATE_FORMAT,
): string {
  return dayjs(date).format(format);
}
export function dateToDuration(ms: number) {
  if (ms === 0 || ms === undefined || ms === null) {
    return '';
  }
  const ss = 1000;
  const mi = ss * 60;
  const hh = mi * 60;
  const dd = hh * 24;

  const day = Math.floor(ms / dd);
  const hour = Math.floor((ms - day * dd) / hh);
  const minute = Math.floor((ms - day * dd - hour * hh) / mi);
  const seconds = Math.floor((ms - day * dd - hour * hh - minute * mi) / ss);
  if (day > 0) {
    return day + 'd ' + hour + 'h ' + minute + 'm ' + seconds + 's';
  } else if (hour > 0) {
    return hour + 'h ' + minute + 'm ' + seconds + 's';
  } else if (minute > 0) {
    return minute + 'm ' + seconds + 's';
  } else {
    return 0 + 'm ' + seconds + 's';
  }
}
export const dateUtil = dayjs;
