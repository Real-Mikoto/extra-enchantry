package realmikoto.extraenchantry.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * 测试用 PNG 结构解析器（纯 Java，无外部依赖）。
 *
 * 逐 chunk 校验到 Minecraft NativeImage(STB) 同等严格度的结构约束：
 * <ul>
 *   <li>文件签名 / chunk 长度不越界（截断文件）；</li>
 *   <li>每个 chunk 的 CRC32；</li>
 *   <li>恰好一个 IHDR；</li>
 *   <li>IDAT 可完整 zlib 解压。</li>
 * </ul>
 * 任一校验失败抛 {@link IllegalStateException}（消息带文件路径），JUnit 直接判失败。
 *
 * 背景：v1.3.2 修复的 resonance.png——旧生成脚本每扫描行多写 1 字节，
 * System.Drawing 能读但 STB 报 "Corrupt PNG"（atlas stitch IOException）。
 * 本解析器按 {@link #expectedRawBytes()} 与 {@link #decompressedRawBytes} 对账，
 * 直接回归该 bug 形态。
 */
public final class TestPng {

	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	/** 图像宽（IHDR，像素） */
	public final int width;
	/** 图像高（IHDR，像素） */
	public final int height;
	/** 位深（IHDR） */
	public final int bitDepth;
	/** 颜色类型（IHDR）：0 灰度 / 2 RGB / 3 调色板 / 4 灰度+Alpha / 6 RGBA */
	public final int colorType;
	/** IDAT 全量 zlib 解压后的字节数（含每行 1 个 filter 字节） */
	public final int decompressedRawBytes;
	/** 依序出现的 chunk 类型列表（诊断信息） */
	public final List<String> chunkTypes;

	private TestPng(int width, int height, int bitDepth, int colorType,
			int decompressedRawBytes, List<String> chunkTypes) {
		this.width = width;
		this.height = height;
		this.bitDepth = bitDepth;
		this.colorType = colorType;
		this.decompressedRawBytes = decompressedRawBytes;
		this.chunkTypes = List.copyOf(chunkTypes);
	}

	/** 解析并结构校验一个 PNG 文件（失败抛 IllegalStateException，消息含路径与原因） */
	public static TestPng read(Path file) {
		byte[] data;
		try {
			data = Files.readAllBytes(file);
		} catch (IOException e) {
			throw new IllegalStateException(file + ": 读取失败 - " + e.getMessage(), e);
		}
		String where = file + ": ";
		if (data.length < PNG_SIGNATURE.length + 12) {
			throw new IllegalStateException(where + "文件过短，不是合法 PNG");
		}
		for (int i = 0; i < PNG_SIGNATURE.length; i++) {
			if (data[i] != PNG_SIGNATURE[i]) {
				throw new IllegalStateException(where + "PNG 签名错误（首 8 字节不符）");
			}
		}

		int pos = 8;
		ByteArrayOutputStream idat = new ByteArrayOutputStream();
		List<String> types = new ArrayList<>();
		int width = 0;
		int height = 0;
		int bitDepth = 0;
		int colorType = 0;
		int ihdrCount = 0;
		while (pos + 8 <= data.length) {
			int length = readUInt32(data, pos);
			String type = new String(data, pos + 4, 4, StandardCharsets.ISO_8859_1);
			int dataStart = pos + 8;
			if (length < 0 || dataStart + length + 4 > data.length) {
				throw new IllegalStateException(where + "chunk " + type + " 长度越界（文件被截断）");
			}
			CRC32 crc = new CRC32();
			crc.update(data, pos + 4, 4 + length);
			long storedCrc = readUInt32(data, dataStart + length) & 0xFFFFFFFFL;
			if (storedCrc != crc.getValue()) {
				throw new IllegalStateException(where + "chunk " + type + " CRC32 校验失败"
						+ "（stored=" + Long.toHexString(storedCrc)
						+ " calc=" + Long.toHexString(crc.getValue()) + "）");
			}
			if ("IHDR".equals(type)) {
				if (length != 13) {
					throw new IllegalStateException(where + "IHDR 长度应为 13，实际 " + length);
				}
				width = readUInt32(data, dataStart);
				height = readUInt32(data, dataStart + 4);
				bitDepth = data[dataStart + 8] & 0xFF;
				colorType = data[dataStart + 9] & 0xFF;
				ihdrCount++;
			} else if ("IDAT".equals(type)) {
				idat.write(data, dataStart, length);
			}
			types.add(type);
			pos = dataStart + length + 4;
			if ("IEND".equals(type)) {
				break;
			}
		}
		if (ihdrCount != 1) {
			throw new IllegalStateException(where + "IHDR 数量应为 1，实际 " + ihdrCount);
		}
		if (idat.size() == 0) {
			throw new IllegalStateException(where + "缺少 IDAT chunk（无像素数据）");
		}
		if (width <= 0 || height <= 0) {
			throw new IllegalStateException(where + "IHDR 尺寸非法：" + width + "x" + height);
		}
		int decompressed = inflateAll(where, idat.toByteArray());
		return new TestPng(width, height, bitDepth, colorType, decompressed, types);
	}

	/** 每像素字节数（按颜色类型；调色板(3)按 1 计；非法类型 -1） */
	public int bytesPerPixel() {
		return switch (colorType) {
			case 0 -> 1; // 灰度
			case 2 -> 3; // RGB
			case 3 -> 1; // 调色板索引
			case 4 -> 2; // 灰度 + Alpha
			case 6 -> 4; // RGBA
			default -> -1;
		};
	}

	/** 期望的解压字节数：高 × (1 filter 字节 + 宽 × 每像素字节) */
	public int expectedRawBytes() {
		int bpp = bytesPerPixel();
		if (bpp <= 0) {
			return -1;
		}
		return height * (1 + width * bpp);
	}

	// ============ 内部 ============

	private static int readUInt32(byte[] data, int offset) {
		return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
				| ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
	}

	private static int inflateAll(String where, byte[] idat) {
		Inflater inflater = new Inflater();
		inflater.setInput(idat);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		try {
			while (!inflater.finished()) {
				int n = inflater.inflate(buffer);
				if (n > 0) {
					out.write(buffer, 0, n);
				} else if (inflater.needsInput() || inflater.needsDictionary()) {
					throw new IllegalStateException(where + "IDAT zlib 流不完整（Corrupt PNG 的典型形态）");
				}
			}
		} catch (DataFormatException e) {
			throw new IllegalStateException(where + "IDAT zlib 数据损坏: " + e.getMessage(), e);
		} finally {
			inflater.end();
		}
		return out.size();
	}
}
