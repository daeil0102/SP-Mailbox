package net.teujaem.mailbox.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * ItemStack &lt;-&gt; Base64 문자열 변환.
 * Bukkit이 기본 제공하는 BukkitObjectOutputStream/InputStream만 사용하므로
 * 별도의 외부 직렬화 라이브러리가 필요 없습니다.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static String serialize(ItemStack item) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(byteStream)) {
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("아이템을 직렬화하지 못했습니다.", e);
        }
    }

    public static ItemStack deserialize(String data) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(byteStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("아이템을 역직렬화하지 못했습니다.", e);
        }
    }
}
