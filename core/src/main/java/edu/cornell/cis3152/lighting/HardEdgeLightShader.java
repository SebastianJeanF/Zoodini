package edu.cornell.cis3152.lighting;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class HardEdgeLightShader {

    public static final String POSITION_ATTRIBUTE = "vertex_positions";
    public static final String COLOR_ATTRIBUTE = "quad_colors";
    public static final String SCALE_ATTRIBUTE = "s";
    public static final String PROJECTION_UNIFORM = "u_projTrans";

    public HardEdgeLightShader() {
    }

    public static final ShaderProgram createLightShader() {
        String gamma = "";
        if (RayHandler.getGammaCorrection()) {
            gamma = "sqrt";
        }

        String[] vertex20 = new String[]{"attribute vec4 vertex_positions;", "attribute vec4 quad_colors;", "attribute float s;", "uniform mat4 u_projTrans;", "varying vec4 v_color;", "void main() {", "    v_color = (s * 50) * quad_colors;", "    gl_Position = u_projTrans * vertex_positions;", "}"};
        String[] vertex30 = new String[]{"in vec4 vertex_positions;", "in vec4 quad_colors;", "in float s;", "uniform mat4 u_projTrans;", "out vec4 v_color;", "void main() {", "    v_color = (s * 50) * quad_colors;", "    gl_Position = u_projTrans * vertex_positions;", "}"};
        String[] fragment20 = new String[]{"#ifdef GL_ES", "    precision lowp float;", "    #define MED mediump", "#else", "    #define MED ", "#endif", "varying vec4 v_color;", "void main() {", "    gl_FragColor = " + gamma + "(v_color);", "}"};
        String[] fragment30 = new String[]{"#ifdef GL_ES", "    precision lowp float;", "    #define MED mediump", "#else", "    #define MED ", "#endif", "out vec4 frag_color;", "in vec4 v_color;", "void main() {", "    frag_color = " + gamma + "(v_color);", "}"};
        String[] vertSource = vertex20;
        String[] fragSource = fragment20;
        if (Gdx.gl30 != null) {
            vertSource = vertex30;
            fragSource = fragment30;
        }

        StringBuilder vstr = new StringBuilder();
        String[] var10 = vertSource;
        int var11 = vertSource.length;

        int var12;
        for(var12 = 0; var12 < var11; ++var12) {
            String s = var10[var12];
            vstr.append(s);
            vstr.append("\n");
        }

        String vertexShader = vstr.toString();
        StringBuilder fstr = new StringBuilder();
        String[] var16 = fragSource;
        var12 = fragSource.length;

        for(int var19 = 0; var19 < var12; ++var19) {
            String s = var16[var19];
            fstr.append(s);
            fstr.append("\n");
        }

        String fragmentShader = fstr.toString();
        ShaderProgram.pedantic = false;
        String vertPrefix = ShaderProgram.prependVertexCode;
        String fragPrefix = ShaderProgram.prependFragmentCode;
        if (Gdx.gl30 != null) {
            ShaderProgram.prependVertexCode = "#version 330 core\n";
            ShaderProgram.prependFragmentCode = "#version 330 core\n";
        }

        ShaderProgram lightShader = new ShaderProgram(vertexShader, fragmentShader);
        if (Gdx.gl30 != null) {
            ShaderProgram.prependVertexCode = vertPrefix;
            ShaderProgram.prependFragmentCode = fragPrefix;
        }

        if (!lightShader.isCompiled()) {
            Gdx.app.error("Light Shader Error", lightShader.getLog());
        }

        return lightShader;
    }
}
