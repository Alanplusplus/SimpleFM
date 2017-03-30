package rui.lin.spectra;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import android.util.Log;

public class MetadataParser {
	protected static int sLogLevel = Log.INFO;
	protected static String sLogTag = "MetadataParser";
	protected static Logger sLogger = new Logger(sLogLevel, sLogTag);

	public static final HashMap<String, String> ID3V2_TAGS = new HashMap<String, String>() {
		{
			put("TIT2", "title");
			put("TALB", "album");
			put("TRCK", "track");
			put("TPE1", "artist");
			put("TXXX", "T_");
			put("WCOM", "commercial_info");
			put("WCOP", "copyright_info");
			put("WOAF", "audio_file_webpage");
			put("WOAR", "artist_webpage");
			put("WOAS", "audio_source_webpage");
			put("WORS", "radio_station_homepage");
			put("WXXX", "W_");
		}
	};

	public static HashMap<String, Vector<String>> parseID3v2(byte[] metadata) {
		try {
			// check ID3v2 mark
			if (metadata != null && metadata[0] == 'I' && metadata[1] == 'D' && metadata[2] == '3') {
				sLogger.info("ID3v2 mark detected");
				// check ID3v2 version
				if (metadata[3] >= 3 && metadata[3] <= 4) {
					sLogger.info("ID3v2 version %d", metadata[3]);
					// get ready-to-parse frame data
					int exthdr_frames_padding_size = (metadata[6] << 21) | (metadata[7] << 14) | (metadata[8] << 7)
							| metadata[9];
					boolean unsych = (metadata[5] & 0x80) != 0;
					boolean exthdr = (metadata[5] & 0x40) != 0;
					int tag_frames_start_pos, tag_frames_size;
					if (exthdr) {
						int exthdr_size = (metadata[10] << 21) | (metadata[11] << 14) | (metadata[12] << 7)
								| metadata[13];
						tag_frames_start_pos = 10 + exthdr_size;
						tag_frames_size = exthdr_frames_padding_size - exthdr_size;
					} else {
						tag_frames_start_pos = 10;
						tag_frames_size = exthdr_frames_padding_size;
					}
					byte[] tag_frames_data = new byte[tag_frames_size];
					if (unsych) {
						int i = 0, j = tag_frames_start_pos;
						while (j < tag_frames_size) {
							if (metadata[j] == 0x00 && metadata[j - 1] == 0xff) {
								j++;
							} else {
								tag_frames_data[i] = metadata[j];
								i++;
								j++;
							}
						}
					} else {
						System.arraycopy(metadata, tag_frames_start_pos, tag_frames_data, 0, tag_frames_size);
					}
					// parse frames
					HashMap<String, Vector<String>> id3v2_kv = new HashMap<String, Vector<String>>();
					int pos = 0;
					int frame_payload_size;
					int frame_encoding;
					Charset charset;
					String frame_id;
					String id3v2_key;
					Vector<String> id3v2_value;
					for (;;) {
						if (tag_frames_size - pos > 10) {
							if (tag_frames_data[pos] >= 'A' && tag_frames_data[pos] <= 'Z') {
								frame_id = new String(tag_frames_data, pos, 4);
								frame_payload_size = (tag_frames_data[pos + 4] << 21)
										| (tag_frames_data[pos + 5] << 14) | (tag_frames_data[pos + 6] << 7)
										| tag_frames_data[pos + 7];
								sLogger.info("TAG %s found with %d byte payload", frame_id, frame_payload_size);
								if (ID3V2_TAGS.containsKey(frame_id)) { // We care about this frame.
									switch (tag_frames_data[pos]) {
									case 'T': // It's a text frame.
										frame_encoding = tag_frames_data[pos + 10];
										switch (frame_encoding) {
										case 0:
											charset = Charset.forName("ISO-8859-1");
											break;
										case 1:
											charset = Charset.forName("UTF-16");
											break;
										case 2:
											charset = Charset.forName("UTF-16BE");
											break;
										case 3:
											charset = Charset.forName("UTF-8");
											break;
										default:
											charset = null;
											break;
										}
										if (charset != null) {
											if (frame_id.equals("TXXX")) { // User defined text frame: one description string, one
																			// value string.
												String[] text_string_list = charset
														.decode(ByteBuffer.wrap(tag_frames_data, pos + 11,
																frame_payload_size - 1)).toString().split("\u0000");
												id3v2_key = ID3V2_TAGS.get(frame_id) + text_string_list[0];
												id3v2_value = new Vector<String>(1);
												id3v2_value.add(text_string_list[1]);
											} else { // Ordinary text frame: multiple value strings
												String[] text_string_list = charset
														.decode(ByteBuffer.wrap(tag_frames_data, pos + 11,
																frame_payload_size - 1)).toString().split("\u0000");
												id3v2_key = ID3V2_TAGS.get(frame_id);
												id3v2_value = new Vector<String>(Arrays.asList(text_string_list));
											}
											id3v2_kv.put(id3v2_key, id3v2_value);
										}
										break;
									case 'W': // It's a URL frame
										if (frame_id.equals("WXXX")) { // User defined URL frame: one description stirng, one URL.
											frame_encoding = tag_frames_data[pos + 10];
											switch (frame_encoding) {
											case 0:
												charset = Charset.forName("ISO-8859-1");
												break;
											case 1:
												charset = Charset.forName("UTF-16");
												break;
											case 2:
												charset = Charset.forName("UTF-16BE");
												break;
											case 3:
												charset = Charset.forName("UTF-8");
												break;
											default:
												charset = null;
												break;
											}
											if (charset != null) {
												String full_decoded_string = charset.decode(
														ByteBuffer.wrap(tag_frames_data, pos + 11,
																frame_payload_size - 1)).toString();
												int pos_of_null = full_decoded_string.indexOf("\u0000");
												String description = full_decoded_string.substring(0, pos_of_null);
												int desc_with_null_byte_count = charset.encode(
														full_decoded_string.substring(0, pos_of_null + 1)).limit();
												id3v2_key = ID3V2_TAGS.get(frame_id) + description;
												id3v2_value = new Vector<String>(1);
												id3v2_value.add(Charset
														.forName("ISO-8859-1")
														.decode(ByteBuffer.wrap(tag_frames_data, pos + 11
																+ desc_with_null_byte_count, frame_payload_size - 1
																- desc_with_null_byte_count)).toString());
												id3v2_kv.put(id3v2_key, id3v2_value);
											}
										} else { // Ordinary URL frame: one URL only
											String[] text_string_list = Charset
													.forName("ISO-8859-1")
													.decode(ByteBuffer.wrap(tag_frames_data, pos + 10,
															frame_payload_size)).toString().split("\u0000");
											id3v2_key = ID3V2_TAGS.get(frame_id);
											id3v2_value = new Vector<String>(1);
											id3v2_value.add(text_string_list[0]);
											id3v2_kv.put(id3v2_key, id3v2_value);
										}
										break;
									default:
										break;
									}
								}
								pos += 10 + frame_payload_size;
								continue;
							}
						}
						break;
					}
					return id3v2_kv;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
