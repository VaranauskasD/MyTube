import YTDlpWrap from "yt-dlp-wrap-plus";
import type { ChannelVideo, YtDlpFlatEntry } from "./types.js";

const ytDlpWrap = new YTDlpWrap.default();

function toVideosUrl(channelUrl: string): string {
  const trimmed = channelUrl.replace(/\/+$/, "");
  return trimmed.endsWith("/videos") ? trimmed : `${trimmed}/videos`;
}

export async function fetchChannelVideos(
  channelUrl: string,
  limit = 10,
): Promise<ChannelVideo[]> {
  const url = toVideosUrl(channelUrl);

  const output = await ytDlpWrap.execPromise([
    url,
    "--flat-playlist",
    "--dump-json",
    "--playlist-end",
    String(limit),
    "--no-warnings",
  ]);

  return output
    .split("\n")
    .filter((line) => line.trim().length > 0)
    .map((line) => JSON.parse(line) as YtDlpFlatEntry)
    .map((entry) => ({
      id: entry.id,
      title: entry.title,
      url: entry.webpage_url ?? entry.url ?? `https://www.youtube.com/watch?v=${entry.id}`,
      uploadDate: entry.upload_date,
      durationSeconds: entry.duration,
      thumbnailUrl: entry.thumbnails?.at(-1)?.url,
    }));
}
