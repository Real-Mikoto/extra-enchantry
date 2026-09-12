package realmikoto.extraenchantry.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 客户端可访问性配置（v1.8.0 §10）：持久化到 config/extra-enchantry-client.properties。
 *
 * - reduceFlashing：降低闪烁（回声视觉脉冲更柔和、峰值更低）；
 * - darknessProtection：黑暗防护（免疫 Boss 无相阶段等强黑暗视觉压迫）。
 *
 * 按键切换即时生效并写回；启动时加载——修复旧版"volatile 静态量断线即重置，
 * 每次进服需重按"的问题。
 */
public final class ClientConfig {

	private static final Path FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
			.getConfigDir().resolve("extra-enchantry-client.properties");

	private ClientConfig() {
	}

	/** 客户端初始化加载（ExtraEnchantryClient.onInitializeClient 调用） */
	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			Properties props = new Properties();
			try (var in = Files.newInputStream(FILE)) {
				props.load(in);
			}
			EchoVisionState.setReduceFlashing(Boolean.parseBoolean(
					props.getProperty("reduceFlashing", "false")));
			EchoVisionState.setDarknessProtection(Boolean.parseBoolean(
					props.getProperty("darknessProtection", "false")));
		} catch (IOException ignored) {
			// 配置读取失败 → 使用默认值
		}
	}

	/** 当前状态写回磁盘（切换即时保存） */
	public static void save() {
		try {
			Properties props = new Properties();
			props.setProperty("reduceFlashing", String.valueOf(EchoVisionState.reduceFlashing()));
			props.setProperty("darknessProtection", String.valueOf(EchoVisionState.darknessProtection()));
			Files.createDirectories(FILE.getParent());
			try (var out = Files.newOutputStream(FILE)) {
				props.store(out, "Extra Enchantry client accessibility settings");
			}
		} catch (IOException ignored) {
			// 配置写入失败 → 下次切换重试
		}
	}
}
