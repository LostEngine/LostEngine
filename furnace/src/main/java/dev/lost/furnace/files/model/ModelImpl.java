package dev.lost.furnace.files.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModelImpl implements Model {

    private final String path;
    String parent;
    GuiLight guiLight;
    Object2ObjectOpenHashMap<String, String> textures = new Object2ObjectOpenHashMap<>();
    ObjectArrayList<Element> elements = new ObjectArrayList<>();
    Display display;
    Boolean ambientocclusion;

    public ModelImpl(@NotNull JsonObject json, String path) {
        this.path = path;
        this.parent = json.get("parent") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        this.guiLight = json.get("gui_light") instanceof JsonPrimitive jp ? GuiLight.valueOf(jp.getAsString().toUpperCase(Locale.ROOT)) : null;
        this.ambientocclusion = json.get("ambientocclusion") instanceof JsonPrimitive jp ? jp.getAsBoolean() : null;
        this.textures = new Object2ObjectOpenHashMap<>();
        if (json.get("textures") instanceof JsonObject texturesJson) {
            for (Map.Entry<String, JsonElement> entry : texturesJson.entrySet()) {
                if (entry.getValue() instanceof JsonPrimitive jp) {
                    textures.put(entry.getKey(), jp.getAsString());
                }
            }
        }
        this.elements = new ObjectArrayList<>();
        if (json.get("elements") instanceof JsonArray elementsJson) {
            for (JsonElement elementJsonElement : elementsJson) {
                if (elementJsonElement instanceof JsonObject elementJson) {
                    Element element = new ModelImpl.ElementImpl()
                            .from(elementJson.get("from").getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray())
                            .to(elementJson.get("to").getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray())
                            .shade(elementJson.get("shade") instanceof JsonPrimitive sp ? sp.getAsBoolean() : null)
                            .lightEmission(elementJson.get("light_emission") instanceof JsonPrimitive lep ? lep.getAsInt() : null);
                    if (elementJson.get("rotation") instanceof JsonObject rotationJson) {
                        Element.Rotation rotation = new ElementImpl.RotationImpl()
                                .origin(rotationJson.get("origin") instanceof JsonArray jsonArray ? jsonArray.getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray() : null)
                                .rescale(rotationJson.get("rescale") instanceof JsonPrimitive rp ? rp.getAsBoolean() : null)
                                .axis(rotationJson.get("axis") instanceof JsonPrimitive ap ? Element.Rotation.Axis.valueOf(ap.getAsString().toUpperCase(Locale.ROOT)) : null)
                                .angle(rotationJson.get("angle") instanceof JsonPrimitive ap ? ap.getAsInt() : 0);
                        element.rotation(rotation);
                    }
                    if (elementJson.get("faces") instanceof JsonObject facesJson) {
                        for (Map.Entry<String, JsonElement> faceEntry : facesJson.entrySet()) {
                            if (faceEntry.getValue() instanceof JsonObject faceJson) {
                                Element.Face face = new ElementImpl.FaceImpl()
                                        .texture(faceJson.get("texture").getAsString())
                                        .uv(faceJson.get("uv") instanceof JsonArray jsonArray ? jsonArray.asList().stream().mapToDouble(JsonElement::getAsDouble).toArray() : null)
                                        .cullface(faceJson.get("cullface") instanceof JsonPrimitive cpf ? Element.Face.FaceType.valueOf(cpf.getAsString().toUpperCase(Locale.ROOT)) : null)
                                        .rotation(faceJson.get("rotation") instanceof JsonPrimitive rp ? rp.getAsFloat() : null)
                                        .tintindex(faceJson.get("tintindex") instanceof JsonPrimitive tip ? tip.getAsInt() : null);
                                element.face(Element.Face.FaceType.valueOf(faceEntry.getKey().toUpperCase(Locale.ROOT)), face);
                            }
                        }
                    }
                }
            }
        }
        if (json.get("display") instanceof JsonObject displayJson) {
            Display display = Display.display();
            for (Map.Entry<String, JsonElement> displayEntry : displayJson.entrySet()) {
                if (displayEntry.getValue() instanceof JsonObject transformJson) {
                    Display.Transform transform = new DisplayImpl.TransformImpl()
                            .rotation(transformJson.get("rotation") instanceof JsonArray jsonArray ? jsonArray.getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray() : null)
                            .translation(transformJson.get("translation") instanceof JsonArray jsonArray ? jsonArray.getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray() : null)
                            .scale(transformJson.get("scale") instanceof JsonArray jsonArray ? jsonArray.getAsJsonArray().asList().stream().mapToDouble(JsonElement::getAsDouble).toArray() : null);
                    display.put(Display.DisplayType.valueOf(displayEntry.getKey().toUpperCase(Locale.ROOT)), transform);
                }
            }
            this.display = display;
        }
    }

    @Override
    public @NotNull JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (parent != null) json.addProperty("parent", parent);
        if (ambientocclusion != null) json.addProperty("ambientocclusion", ambientocclusion);
        if (guiLight != null) json.addProperty("gui_light", guiLight.toString().toLowerCase());
        if (!textures.isEmpty()) {
            JsonObject texturesJson = new JsonObject();
            for (Map.Entry<String, String> entry : textures.entrySet()) {
                texturesJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("textures", texturesJson);
        }
        if (!elements.isEmpty()) {
            JsonArray elementsJson = new JsonArray();
            for (Element element : elements) {
                JsonObject elementJson = new JsonObject();
                elementJson.add("from", arrayToJson(element.from()));
                elementJson.add("to", arrayToJson(element.to()));
                if (element.shade() != null) elementJson.addProperty("shade", element.shade());
                if (element.lightEmission() != null) elementJson.addProperty("light_emission", element.lightEmission());
                if (element.faces() != null) {
                    JsonObject facesJson = new JsonObject();
                    for (Map.Entry<Element.Face.FaceType, Element.Face> faceEntry : element.faces().entrySet()) {
                        Element.Face face = faceEntry.getValue();
                        JsonObject faceJson = new JsonObject();
                        faceJson.addProperty("texture", face.texture());
                        if (face.uv() != null) faceJson.add("uv", arrayToJson(face.uv()));
                        if (face.cullface() != null) faceJson.addProperty("cullface", face.cullface().toString().toLowerCase());
                        if (face.rotation() != null) faceJson.addProperty("rotation", face.rotation());
                        if (face.tintindex() != null) faceJson.addProperty("tintindex", face.tintindex());
                        facesJson.add(faceEntry.getKey().toString().toLowerCase(), faceJson);
                    }
                    elementJson.add("faces", facesJson);
                }
                if (element.rotation() != null) {
                    JsonObject rotationJson = new JsonObject();
                    rotationJson.add("origin", arrayToJson(element.rotation().origin()));
                    if (element.rotation().rescale() != null)
                        rotationJson.addProperty("rescale", element.rotation().rescale());
                    if (element.rotation().axis() != null)
                        rotationJson.addProperty("axis", element.rotation().axis().toString().toLowerCase());
                    rotationJson.addProperty("angle", element.rotation().angle());
                    elementJson.add("rotation", rotationJson);
                }
                elementsJson.add(elementJson);
            }
            json.add("elements", elementsJson);
        }
        if (display != null && !display.isEmpty()) {
            JsonObject displayJson = new JsonObject();
            for (Map.Entry<Display.DisplayType, Display.Transform> displayEntry : display.entrySet()) {
                Display.Transform transform = displayEntry.getValue();
                JsonObject transformJson = new JsonObject();
                if (transform.rotation() != null) transformJson.add("rotation", arrayToJson(transform.rotation()));
                if (transform.translation() != null) transformJson.add("translation", arrayToJson(transform.translation()));
                if (transform.scale() != null) transformJson.add("scale", arrayToJson(transform.scale()));
                displayJson.add(displayEntry.getKey().toString().toLowerCase(), transformJson);
            }
            json.add("display", displayJson);
        }
        return json;
    }

    private static @NotNull JsonArray arrayToJson(float @NotNull [] array) {
        JsonArray jsonArray = new JsonArray();
        for (float i : array) {
            jsonArray.add(i);
        }
        return jsonArray;
    }

    public ModelImpl(String path) {
        this.path = path;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String parent() {
        return parent;
    }

    @Override
    public Model parent(String parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public GuiLight guiLight() {
        return guiLight;
    }

    @Override
    public Model guiLight(GuiLight guiLight) {
        this.guiLight = guiLight;
        return this;
    }

    @Override
    public Map<String, String> textures() {
        return textures;
    }

    @Override
    public Model texture(String key, String value) {
        textures.put(key, value);
        return this;
    }

    @Override
    public List<Element> elements() {
        return elements;
    }

    @Override
    public Model element(Element element) {
        elements.add(element);
        return this;
    }

    @Override
    public Display display() {
        return display;
    }

    @Override
    public Model display(Display display) {
        this.display = display;
        return this;
    }

    @Override
    public Boolean ambientocclusion() {
        return ambientocclusion;
    }

    @Override
    public Model ambientocclusion(Boolean ambientocclusion) {
        this.ambientocclusion = ambientocclusion;
        return this;
    }

    static class ElementImpl implements Element {
        float[] from;
        float[] to;
        Map<Face.FaceType, Face> faces;
        Rotation rotation;
        Boolean shade;
        Integer lightEmission;

        @Override
        public float[] from() {
            return from;
        }

        @Override
        public float[] to() {
            return to;
        }

        @Override
        public Map<Face.FaceType, Face> faces() {
            return faces;
        }

        @Override
        public Rotation rotation() {
            return rotation;
        }

        @Override
        public Boolean shade() {
            return shade;
        }

        @Override
        public Integer lightEmission() {
            return lightEmission;
        }

        @Override
        public ElementImpl from(float[] from) {
            this.from = from;
            return this;
        }

        public ElementImpl from(double[] from) {
            if (from == null) {
                this.from = null;
            } else {
                this.from = new float[from.length];
                for (int i = 0; i < from.length; i++) {
                    this.from[i] = (float) from[i];
                }
            }
            return this;
        }

        @Override
        public ElementImpl to(float[] to) {
            this.to = to;
            return this;
        }

        public ElementImpl to(double[] to) {
            if (to == null) {
                this.to = null;
            } else {
                this.to = new float[to.length];
                for (int i = 0; i < to.length; i++) {
                    this.to[i] = (float) to[i];
                }
            }
            return this;
        }

        @Override
        public ElementImpl face(Face.FaceType faceType, Face face) {
            faces.put(faceType, face);
            return this;
        }

        @Override
        public ElementImpl rotation(Rotation rotation) {
            this.rotation = rotation;
            return this;
        }

        @Override
        public ElementImpl shade(Boolean shade) {
            this.shade = shade;
            return this;
        }

        @Override
        public ElementImpl lightEmission(Integer lightEmission) {
            this.lightEmission = lightEmission;
            return this;
        }

        static class RotationImpl implements Rotation {
            float[] origin;
            Boolean rescale;
            Axis axis;
            float angle;

            @Override
            public float[] origin() {
                return origin;
            }

            @Override
            public Boolean rescale() {
                return rescale;
            }

            @Override
            public Axis axis() {
                return axis;
            }

            @Override
            public float angle() {
                return angle;
            }

            @Override
            public RotationImpl origin(float[] origin) {
                this.origin = origin;
                return this;
            }

            public RotationImpl origin(double[] origin) {
                if (origin == null) {
                    this.origin = null;
                } else {
                    this.origin = new float[origin.length];
                    for (int i = 0; i < origin.length; i++) {
                        this.origin[i] = (float) origin[i];
                    }
                }
                return this;
            }

            @Override
            public RotationImpl rescale(Boolean rescale) {
                this.rescale = rescale;
                return this;
            }

            @Override
            public RotationImpl axis(Axis axis) {
                this.axis = axis;
                return this;
            }

            @Override
            public RotationImpl angle(float angle) {
                this.angle = angle;
                return this;
            }
        }

        static class FaceImpl implements Face {
            String texture;
            float[] uv;
            FaceType cullface;
            Float rotation;
            Integer tintindex;

            @Override
            public String texture() {
                return texture;
            }

            @Override
            public float[] uv() {
                return uv;
            }

            @Override
            public FaceType cullface() {
                return cullface;
            }

            @Override
            public Float rotation() {
                return rotation;
            }

            @Override
            public Integer tintindex() {
                return tintindex;
            }

            @Override
            public FaceImpl texture(String texture) {
                this.texture = texture;
                return this;
            }

            @Override
            public FaceImpl uv(float[] uv) {
                this.uv = uv;
                return this;
            }

            public FaceImpl uv(double[] uv) {
                if (uv == null) {
                    this.uv = null;
                } else {
                    this.uv = new float[uv.length];
                    for (int i = 0; i < uv.length; i++) {
                        this.uv[i] = (float) uv[i];
                    }
                }
                return this;
            }

            @Override
            public FaceImpl cullface(FaceType cullface) {
                this.cullface = cullface;
                return this;
            }

            @Override
            public FaceImpl rotation(Float rotation) {
                this.rotation = rotation;
                return this;
            }

            @Override
            public FaceImpl tintindex(Integer tintindex) {
                this.tintindex = tintindex;
                return this;
            }
        }
    }

    static class DisplayImpl extends Object2ObjectOpenHashMap<Display.DisplayType, Display.Transform> implements Display {
        static class TransformImpl implements Display.Transform {
            float[] rotation;
            float[] translation;
            float[] scale;

            @Override
            public float[] rotation() {
                return rotation;
            }

            @Override
            public float[] translation() {
                return translation;
            }

            @Override
            public float[] scale() {
                return scale;
            }

            @Override
            public TransformImpl rotation(float[] rotation) {
                this.rotation = rotation;
                return this;
            }

            public TransformImpl rotation(double[] rotation) {
                if (rotation == null) {
                    this.rotation = null;
                } else {
                    this.rotation = new float[rotation.length];
                    for (int i = 0; i < rotation.length; i++) {
                        this.rotation[i] = (float) rotation[i];
                    }
                }
                return this;
            }

            @Override
            public TransformImpl translation(float[] translation) {
                this.translation = translation;
                return this;
            }

            public TransformImpl translation(double[] translation) {
                if (translation == null) {
                    this.translation = null;
                } else {
                    this.translation = new float[translation.length];
                    for (int i = 0; i < translation.length; i++) {
                        this.translation[i] = (float) translation[i];
                    }
                }
                return this;
            }

            @Override
            public TransformImpl scale(float[] scale) {
                this.scale = scale;
                return this;
            }

            public TransformImpl scale(double[] scale) {
                if (scale == null) {
                    this.scale = null;
                } else {
                    this.scale = new float[scale.length];
                    for (int i = 0; i < scale.length; i++) {
                        this.scale[i] = (float) scale[i];
                    }
                }
                return this;
            }
        }
    }
}
