package glGraphics;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * ShaderWrapper collect all data there is about a glsl shader.
 * Upon initializing, two text-files are read, one for the Vertex Shader,
 * one for the Fragment Shader. Both are then compiled and linked into a
 * working shaderProgram, storing its ID for further use by external sources.
 * 
 * Upon the process of reading the text files, a very crude syntax detection is
 * applied, as to detect all uniform variables. Uniforms are special variables for
 * a shader, that can be read by any fragment. They are similar to global variables
 * for shader programs.
 * 
 * This process is crude and error prone, but enhances quality of life, if simple rules
 * are followed when writing glsl code: when declaring uniform variables, it is expected
 * to be one identifier per line, in the same line as the word "uniform" appearers.
 * Only types allowed are:
 * 
 * sampler2D
 * int
 * float
 * vec[2-4]f
 * mat4
 * 
 * When the glsl code abides these rules, information about the uniforms gets stored in the
 * ShaderWrapper, allowing a very easy method of setting those uniforms, aswell as getting
 * basic informations about them (name & type)
 * 
 */

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.SimpleLogger;

public class ShaderWrapper {
  
  private static FloatBuffer matrixBuffer; //avoid new buffer allocation for every single object drawn
  
  private int shaderID = -1;
  private HashMap<String, Integer> uniformType;
  private HashMap<String, Integer> uniformHandle;
  
  private String vertexShader;
  private String fragmentShader;
  
  public ShaderWrapper(String vs, String fs){
    if(matrixBuffer == null) matrixBuffer = (ByteBuffer.allocateDirect(16*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asFloatBuffer();
    
    try {
      vertexShader = readShaderFile(vs);
      fragmentShader = readShaderFile(fs);
      
      shaderID = compileShader(vertexShader, fragmentShader);
      
      detectUniforms();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private void detectUniforms(){
    uniformType = new HashMap<>();
    uniformHandle = new HashMap<>();
    
    String workplace = vertexShader+"\n"+fragmentShader;
    
    String[] lineSet = workplace.split("\n");
    String[] wordSet;
    String uniName;
    int wsIdx;
    boolean hit;
    
    SimpleLogger.log("lineSet size="+lineSet.length, 10, ShaderWrapper.class, "detectUniforms");
    for(String s : lineSet){
      wordSet = s.split(" ");
      wsIdx = 0;
      hit = false;
      
      while(wsIdx < wordSet.length && !hit){
        if (wordSet[wsIdx].equals("uniform")) hit = true;
        else wsIdx++;
      }
      if(hit){
        uniName = wordSet[wsIdx+2];
        uniName = uniName.substring(0, uniName.length()-1);//remove semicolon
        uniformType.put(uniName, getVarIDX(wordSet[wsIdx+1]));
        uniformHandle.put(uniName, glGetUniformLocation(shaderID, uniName));
      }
      
    }
    SimpleLogger.log("Detected "+uniformType.size()+" uniforms.", 10, ShaderWrapper.class, "detectUniforms");
    printUniforms(10);
  }
  
  @SuppressWarnings("unchecked")
  public HashMap<String, Integer> getUniformNames(){
    return (HashMap<String, Integer>)uniformType.clone();
  }
  
  public void storeUniform(String name, Object value){
    glUseProgram(shaderID);
    
    if(uniformType.containsKey(name)){
      int type = uniformType.get(name);
      int handle = uniformHandle.get(name);
      
      switch(type){
      case 0: storeInteger(handle, (int)    value);  break;
      case 1: storeFloat(  handle, (float)     value);  break;
      case 2: storeVector( handle, (Vector2f)  value);  break;
      case 3: storeVector( handle, (Vector3f)  value);  break;
      case 4: storeVector( handle, (Vector4f)  value);  break;
      case 5: storeMatrix( handle, (Matrix4f)  value);  break;
      default:SimpleLogger.log("Unsupported type ("+type+")", -1, ShaderWrapper.class, "storeUniform");break;
      }
    }else if(!name.equals("fboTex")){
      SimpleLogger.log("Warning: Shader has no uniform named "+name, 0, ShaderWrapper.class, "storeUniform");
    }else{
      SimpleLogger.log("Shader has no uniform named fboTex. (Probalby attempt from FrameBufferManager", 10, ShaderWrapper.class, "storeUniform");
    }
  }
  
  public int getShaderID(){
    return shaderID;
  }
  
  public void printUniforms(int logLevel){
    if(SimpleLogger.level >= logLevel){
      for(Entry<String, Integer> e : uniformType.entrySet()){
        SimpleLogger.log(getVarName(e.getValue())+" "+e.getKey(), logLevel, ShaderWrapper.class, "printUniforms");
      }
    }
  }
  
  
  public static int getVarIDX(String varType){
    if(varType.equals("sampler2D")) return 0;
    if(varType.equals("int"))     return 0;
    if(varType.equals("float"))   return 1;
    if(varType.equals("vec2"))    return 2;
    if(varType.equals("vec3"))    return 3;
    if(varType.equals("vec4"))    return 4;
    if(varType.equals("mat4"))    return 5;
    else              return -1;
  }
  
  public static String getVarName(int varIDX){
    switch(varIDX){
    case 0: return "int";
    case 1: return "float";
    case 2: return "vec2";
    case 3: return "vec3";
    case 4: return "vec4";
    case 5: return "mat4";
    default: return "unknown";
    }
  }
  
  private static void storeMatrix(int uniformHandle, Matrix4f mat){
    mat.store(matrixBuffer);
    matrixBuffer.flip();
    glUniformMatrix4(uniformHandle, false, matrixBuffer);
  }
  
  private static void storeInteger(int uniformHandle, int i){
    glUniform1i(uniformHandle, i);
  }
  
  private static void storeFloat(int uniformHandle, float f){
    glUniform1f(uniformHandle, f);
  }
  
  private static void storeVector(int uniformHandle, Vector2f vec){
    glUniform2f(uniformHandle, vec.x, vec.y);
  }
  
  private static void storeVector(int uniformHandle, Vector3f vec){
    glUniform3f(uniformHandle, vec.x, vec.y, vec.z);
  }
  
  private static void storeVector(int uniformHandle, Vector4f vec){
    glUniform4f(uniformHandle, vec.x, vec.y, vec.z, vec.w);
  }
  
  private static String readShaderFile(String fn) throws IOException{
    try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fn)));){
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while(line!=null){
          sb.append(line+"\n");
          line = br.readLine();
        }
        br.close();
        return sb.toString();
    }
  }
  
  private static int compileShader(String vertex, String fragment){
    int IDX_v = glCreateShader(GL_VERTEX_SHADER);
    //set Text source
    glShaderSource(IDX_v, vertex);
    //compile
    glCompileShader(IDX_v);
    
    //retrieve status
    int status = glGetShaderi(IDX_v, GL_COMPILE_STATUS);
    if(status == GL_TRUE)    SimpleLogger.log("V_SH: SUCCESSFULL", 10, ShaderWrapper.class, "compileShader");
    else            throw new OpenGLException("ERROR:\n" + glGetShaderInfoLog(IDX_v, 512));
    
    int IDX_f = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(IDX_f, fragment);
    glCompileShader(IDX_f);
    status = glGetShaderi(IDX_f, GL_COMPILE_STATUS);
    if(status == GL_TRUE)    SimpleLogger.log("F_SH: SUCCESSFULL", 10, ShaderWrapper.class, "compileShader");
    else            throw new OpenGLException("ERROR:\n" + glGetShaderInfoLog(IDX_f, 512));
    
    //generate shaderProgram ID
    int IDX_prog =glCreateProgram();
    //bind the shaders to it
    glAttachShader(IDX_prog, IDX_v);
    glAttachShader(IDX_prog, IDX_f);
    //specify to what output the fragment shader writes, as that can be multiple outputs, technically not needed with onyl 1 output
    glBindFragDataLocation(IDX_prog, 0, "outColor");
    //after compilation of the parts, link everything together
    glLinkProgram(IDX_prog);
    //free compiler data
    glDetachShader(IDX_prog, IDX_v);
    glDetachShader(IDX_prog, IDX_f);
    glDeleteShader(IDX_v);
    glDeleteShader(IDX_f);
    
    return IDX_prog;
  }
  
  public void releaseShader(){
    glDeleteProgram(shaderID);
  }
}
