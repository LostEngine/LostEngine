package dev.lost.furnace.files.model;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface Model {

    @NotNull JsonObject toJson();

    String path();

    String parent();

    Model parent(String parent);

    GuiLight guiLight();

    Model guiLight(GuiLight guiLight);

    Map<String, String> textures();

    Model texture(String key, String value);

    List<Element> elements();

    Model element(Element element);

    Display display();

    Model display(Display display);

    Boolean ambientocclusion();

    Model ambientocclusion(Boolean ambientocclusion);

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull Model model(JsonObject json, String path) {
        return new ModelImpl(json, path);
    }

    @Contract(value = "_ -> new", pure = true)
    static @NotNull Model model(String path) {
        return new ModelImpl(path);
    }

    enum GuiLight {
        FRONT,
        SIDE
    }

    interface Element {

        @Contract(value = " -> new", pure = true)
        static @NotNull Element element() {
            return new ModelImpl.ElementImpl();
        }

        float[] from();

        float[] to();

        Map<Face.FaceType, Face> faces();

        Rotation rotation();

        Boolean shade();

        Integer lightEmission();

        Element from(float[] from);

        Element to(float[] to);

        Element face(Face.FaceType faceType, Face face);

        Element rotation(Rotation rotation);

        Element shade(Boolean shade);

        Element lightEmission(Integer lightEmission);

        interface Rotation {

            @Contract(value = " -> new", pure = true)
            static @NotNull Rotation rotation() {
                return new ModelImpl.ElementImpl.RotationImpl();
            }

            float[] origin();

            Boolean rescale();

            Axis axis();

            float angle();

            Rotation origin(float[] origin);

            Rotation rescale(Boolean rescale);

            Rotation axis(Axis axis);

            Rotation angle(float angle);

            enum Axis {
                X,
                Y,
                Z
            }
        }

        interface Face {

            @Contract(value = " -> new", pure = true)
            static @NotNull Face face() {
                return new ModelImpl.ElementImpl.FaceImpl();
            }

            enum FaceType {
                DOWN,
                UP,
                NORTH,
                SOUTH,
                WEST,
                EAST
            }

            String texture();

            float[] uv();

            FaceType cullface();

            Float rotation();

            Integer tintindex();

            Face texture(String texture);

            Face uv(float[] uv);

            Face cullface(FaceType cullface);

            Face rotation(Float rotation);

            Face tintindex(Integer tintindex);
        }

    }

    interface Display extends Map<Display.DisplayType, Display.Transform> {

        @Contract(value = " -> new", pure = true)
        static @NotNull Display display() {
            return new ModelImpl.DisplayImpl();
        }

        enum DisplayType {
            FIRSTPERSON_RIGHTHAND,
            FIRSTPERSON_LEFTHAND,
            THIRDPERSON_RIGHTHAND,
            THIRDPERSON_LEFTHAND,
            GUI,
            HEAD,
            GROUND,
            FIXED,
            ON_SHELF
        }

        interface Transform {

            @Contract(value = " -> new", pure = true)
            static @NotNull Transform transform() {
                return new ModelImpl.DisplayImpl.TransformImpl();
            }

            float[] rotation();

            float[] translation();

            float[] scale();

            Transform rotation(float[] rotation);

            Transform translation(float[] translation);

            Transform scale(float[] scale);
        }

    }
}
