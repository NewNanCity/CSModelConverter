package io.github.gk0wk.CSModelConverter;

import com.alibaba.fastjson.JSONObject;
import com.mia.craftstudio.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws IOException {
        File baseDir = new File("./Models");
        if (!baseDir.exists() || !baseDir.isDirectory())
            return;
        for (File file : Objects.requireNonNull(baseDir.listFiles())) {
            if (!file.isFile() || !file.getCanonicalPath().endsWith(".csmodel"))
                continue;
            ByteBuffer buffer = getBuffer(new FileInputStream(file));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            CSModel model = new CSModel(buffer);
            File exportDir = new File(file.getAbsolutePath().replaceFirst("\\.csmodel$", "/"));
            if (!exportDir.exists() && !exportDir.mkdirs())
                continue;
            exportModel(model, exportDir);
        }
    }

    private static void exportModel(CSModel model, File exportDir) throws IOException {
        JSONObject jsonObject = new JSONObject();
        List<Integer> animationAssetIDS = new ArrayList<>();
        for (short value : model.getAnimationAssetIDS()) {
            animationAssetIDS.add((int) value);
        }
        jsonObject.put("animation-asset", animationAssetIDS);
        jsonObject.put("transparency", model.hasTransparency());
        List<JSONObject> topNodes = new ArrayList<>();
        model.getTopNodes().forEach(node -> topNodes.add(parseNode(node)));
        jsonObject.put("nodes", topNodes);

        PrintWriter writer = new PrintWriter(new File(exportDir, "info.json"));
        writer.print(jsonObject.toString());
        writer.close();

        ImageIO.write(model.getTexture(), "png", new File(exportDir, "texture.png"));
    }

    private static JSONObject parseNode(CSModel.ModelNode node) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", node.getNodeID());
        jsonObject.put("name", node.getCleanName());
        jsonObject.put("position", node.getPosition());
        jsonObject.put("offset", node.getOffset());
        jsonObject.put("scale", node.getScale());
        jsonObject.put("size", node.getSize());
        jsonObject.put("quads", node.getQuads());

        JSONObject orientationObject = new JSONObject();
        orientationObject.put("x", node.getOrientation().x);
        orientationObject.put("y", node.getOrientation().y);
        orientationObject.put("z", node.getOrientation().z);
        orientationObject.put("w", node.getOrientation().w);
        jsonObject.put("orientation", orientationObject);

        List<Integer> uvTransform = new ArrayList<>();
        for (byte value : node.getUVTransform()) {
            uvTransform.add((int) value);
        }
        jsonObject.put("uv-transform", uvTransform);

        List<JSONObject> children = new ArrayList<>();
        node.getChildren().forEach(child -> children.add(parseNode(child)));
        jsonObject.put("children", children);

        return jsonObject;
    }

    private static ByteBuffer getBuffer(InputStream in) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int len;
        while ((len = in.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
            if (len < 2048) {
                break;
            }
        }
        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    }
}
