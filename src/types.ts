export interface ChannelVideo {
  id: string;
  title: string;
  url: string;
  uploadDate?: string;
  durationSeconds?: number;
  thumbnailUrl?: string;
}

export interface YtDlpFlatEntry {
  id: string;
  title: string;
  url?: string;
  webpage_url?: string;
  upload_date?: string;
  duration?: number;
  thumbnails?: { url: string }[];
}
