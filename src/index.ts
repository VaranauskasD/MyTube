import { fetchChannelVideos } from "./channel.js";

async function main() {
  const channelUrl = process.argv[2] ?? "https://www.youtube.com/@budgetforlifers";
  const limit = Number(process.argv[3] ?? 10);

  console.log(`Fetching latest ${limit} videos for ${channelUrl}\n`);

  const videos = await fetchChannelVideos(channelUrl, limit);

  for (const video of videos) {
    console.log(`${video.uploadDate ?? "unknown date"}  ${video.title}`);
    console.log(`  ${video.url}\n`);
  }
}

main().catch((err) => {
  console.error("Failed to fetch channel videos:", err);
  process.exitCode = 1;
});
