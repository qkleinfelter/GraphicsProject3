import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.*;

/**
 * @author Jerry Heuring
 *
 *         9/20/2021 : Updated to do 3 views and rotation using arrow keys.
 *         9/20/2021 : Fixed error in call to glDrawArrays() -- number of
 *         vertices was incorrect.
 * 
 */
public class Snow {
	private interface Buffer {

		int MAX = 4;
	}

	/**
	 * Created by GBarbieri on 16.03.2017.
	 * 
	 * Program heavily modified by Jerry Heuring in September 2021. Most
	 * modifications stripped out code that was not yet needed, reorganized the
	 * remaining code to more closely align with the C/C++ version of the initial
	 * program.
	 */
	public class HelloTriangleSimple implements GLEventListener, KeyListener {

		private GLWindow window;
		private Animator animator;

		public void main(String[] args) {
			new HelloTriangleSimple().setup();
		}

		private final int[] nbrVertices = new int [4];
		private final IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
		private final IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(4);
		private Program program;
		private final PMVMatrix rotationMatrix = new PMVMatrix();
		private final PMVMatrix viewMatrix = new PMVMatrix();
		private final PMVMatrix projectionMatrix = new PMVMatrix();
		private boolean useInstanced = false;
		private final ParticleInterface flakes = new SnowParticle();

		private void setup() {

			GLProfile glProfile = GLProfile.get(GLProfile.GL4);
			GLCapabilities glCapabilities = new GLCapabilities(glProfile);

			window = GLWindow.create(glCapabilities);

			window.setTitle("Moving Objects Demo");
			window.setSize(600, 600);

			window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
			window.setVisible(true);

			window.addGLEventListener(this);
			window.addKeyListener(this);

			animator = new Animator(window);
			animator.start();

			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowDestroyed(WindowEvent e) {
					animator.stop();
					System.exit(1);
				}
			});
		}

		@Override
		public void init(GLAutoDrawable drawable) {

			GL4 gl = drawable.getGL().getGL4();

			initDebug(gl);
			program = new Program(gl, "src/", "passthrough", "directional");
			rotationMatrix.glLoadIdentity();
			viewMatrix.glLoadIdentity();
			viewMatrix.gluLookAt(0.0f, 0.0f, 25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			
			projectionMatrix.glLoadIdentity();
			projectionMatrix.gluPerspective(60.0f, 1.0f, 0.01f, 1000.0f);

			buildObjects(gl);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
		}

		private void initDebug(GL4 gl) {

			window.getContext().addGLDebugListener(System.out::println);
			/*
			 * sets up medium and high severity error messages to be printed.
			 */
			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, null, false);

			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_HIGH, 0, null, true);

			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_MEDIUM, 0, null, true);
		}

		private void buildObjects(GL4 gl) {
			flakes.init(5000);  // set up for 5000 snowflakes (maximum).
			
			OBJinfo snowflake = new OBJinfo();
			snowflake.readOBJFile("src/cow.obj");
			FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(snowflake.getVertexList());
			FloatBuffer normalBuffer = GLBuffers.newDirectFloatBuffer(snowflake.getNormalList());
			System.out.println("vertexBuffer Capacity = " + vertexBuffer.capacity() + "  normalBuffer Capacity = " + normalBuffer.capacity());

			gl.glGenVertexArrays(1, vertexArrayName);
			gl.glBindVertexArray(vertexArrayName.get(0));
			gl.glGenBuffers(Buffer.MAX, bufferName);
			gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
			gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L + normalBuffer.capacity() * 4L, null, GL_STATIC_DRAW);
			gl.glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer.capacity() * 4L, vertexBuffer);
			gl.glBufferSubData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L, normalBuffer.capacity() * 4L, normalBuffer);
			nbrVertices[0] = vertexBuffer.capacity() / 4;
			int vPosition = gl.glGetAttribLocation(program.name, "vPosition");
			int vNormal = gl.glGetAttribLocation(program.name, "vNormal");
			gl.glEnableVertexAttribArray(vPosition);
			gl.glVertexAttribPointer(vPosition, 4, GL_FLOAT, false, 0, 0);
			if (vNormal != -1) {
				gl.glEnableVertexAttribArray(vNormal);
				gl.glVertexAttribPointer(vNormal, 3, GL_FLOAT, false, 0, vertexBuffer.capacity() * 4L);
			}
//			int pTranslationLocation = gl.glGetAttribLocation(program.name, "pTranslation");
//			if (pTranslationLocation != -1) {
//				gl.glEnableVertexAttribArray(pTranslationLocation);
//				gl.glVertexAttribPointer(pTranslationLocation, 3, GL_FLOAT, false, 0, positionInfo);
//			}
		}
			
	
		
		@Override
		/*
		 * Display the object. 
		 * 
		 * @see
		 * com.jogamp.opengl.GLEventListener#display(com.jogamp.opengl.GLAutoDrawable)
		 */
		public void display(GLAutoDrawable drawable) {

			GL4 gl = drawable.getGL().getGL4();

			gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			gl.glUseProgram(program.name);
			setupDirectedLights(gl);
			flakes.generate(5);
			gl.glBindVertexArray(vertexArrayName.get(0));
			gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
			int modelMatrixLocation = gl.glGetUniformLocation(program.name, "modelingMatrix");
			gl.glUniformMatrix4fv(modelMatrixLocation, 1, false, rotationMatrix.glGetMatrixf());
			int viewMatrixLocation = gl.glGetUniformLocation(program.name, "viewingMatrix");
			gl.glUniformMatrix4fv(viewMatrixLocation, 1, false, viewMatrix.glGetMatrixf());
			int projectionMatrixLocation = gl.glGetUniformLocation(program.name, "projectionMatrix");
			gl.glUniformMatrix4fv(projectionMatrixLocation, 1, false, projectionMatrix.glGetMatrixf());
			int normalMatrixLocation = gl.glGetUniformLocation(program.name, "normalMatrix");
			
			// draw cows as snow
			gl.glBindVertexArray(vertexArrayName.get(0));
			gl.glBindBuffer(GL_ARRAY_BUFFER,  bufferName.get(0));
			PMVMatrix snowScale = new PMVMatrix();
			snowScale.glScalef(0.3f, 0.3f, 0.3f);
			float[] snowTranslation = flakes.getPositions();
			
			PMVMatrix snowTranslate = new PMVMatrix();
			for (int current = 0; current < flakes.getNumberOfParticles(); current++) {
				snowTranslate.glLoadIdentity();
				snowTranslate.glTranslatef(snowTranslation[current*3], snowTranslation[current*3+1], snowTranslation[current*3+2]);
				PMVMatrix snowModel = new PMVMatrix();
				snowModel.glLoadIdentity();
				snowModel.glMultMatrixf(snowTranslate.glGetMatrixf());
				snowModel.glMultMatrixf(snowScale.glGetMatrixf());
				gl.glUniformMatrix4fv(modelMatrixLocation, 1, false, snowModel.glGetMatrixf());
				gl.glUniformMatrix4fv(normalMatrixLocation, 1, false, snowModel.glGetMvitMatrixf());
				gl.glDrawArrays(GL_TRIANGLES,  0,  nbrVertices[0]);
			}
			flakes.update(0.033f);
			flakes.compact();
		}

		/**
		 * This method sets up the lighting information for the directed lights
		 * for this application.  
		 * 
		 * @param gl  -- opengl context
		 */
		private void setupDirectedLights(GL4 gl) {
			int ambientLightLocation = gl.glGetUniformLocation(program.name, "ambientLight");
			int lightDirectionLocation = gl.glGetUniformLocation(program.name, "lightDirection");
			int lightColorLocation = gl.glGetUniformLocation(program.name, "lightColor");
			int shininessLocation = gl.glGetUniformLocation(program.name, "shininess");
			int strengthLocation = gl.glGetUniformLocation(program.name, "strength");
			int halfVectorLocation = gl.glGetUniformLocation(program.name, "halfVector");
			float[] ambientLight = {0.4f, 0.4f, 0.4f};
			float[] lightDirection = {0.0f, 0.7071f, 0.7071f};
			float[] lightColor = { 0.5f, 0.5f, 0.5f };
			float[] halfVector = {0.0f, 0.45514f, 0.9240f };
			float strength = 1.0f;
			float shininess = 25.0f;
			gl.glUniform1f(shininessLocation, shininess);
			gl.glUniform1f(strengthLocation, strength);
			gl.glUniform3f(halfVectorLocation, halfVector[0], halfVector[1], halfVector[2]);
			gl.glUniform3f(lightColorLocation, lightColor[0], lightColor[1], lightColor[2]);
			gl.glUniform3f(lightDirectionLocation, lightDirection[0], lightDirection[1], lightDirection[2]);
			gl.glUniform3f(ambientLightLocation, ambientLight[0], ambientLight[1], ambientLight[2]);
		}

		@Override
		/*
		 * handles window reshapes -- it should affect the size of the view as well so
		 * that things remain square but since we haven't gotten to projections yet it
		 * does not.
		 * 
		 * @see
		 * com.jogamp.opengl.GLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable,
		 * int, int, int, int)
		 */
		public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

			GL4 gl = drawable.getGL().getGL4();
			gl.glViewport(x, y, width, height);
		}

		@Override
		/*
		 * This method disposes of resources cleaning up at the end. This wasn't
		 * happening in the C/C++ version but would be a good idea.
		 */
		public void dispose(GLAutoDrawable drawable) {
			GL4 gl = drawable.getGL().getGL4();

			gl.glDeleteProgram(program.name);
			gl.glDeleteVertexArrays(1, vertexArrayName);
			gl.glDeleteBuffers(Buffer.MAX, bufferName);
		}

		@Override
		/*
		 * Keypress callback for java -- handle a keypress (non-Javadoc)
		 * 
		 * @see
		 * com.jogamp.newt.event.KeyListener#keyPressed(com.jogamp.newt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				new Thread(() -> window.destroy()).start();
			} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rotationMatrix.glRotatef(10.0f, 0.0f, 1.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				rotationMatrix.glRotatef(-10.0f, 0.0f, 1.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_X) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_Z) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(0.0f, 0.0f, 25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_Y) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(0.0f, 25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_O) {
				projectionMatrix.glLoadIdentity();
				projectionMatrix.glOrthof(-100.0f, 100.0f, -100.0f, 100.0f, -100.0f, 100.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_P) {
				projectionMatrix.glLoadIdentity();
				projectionMatrix.gluPerspective(60.0f, 1.0f, 0.01f, 1000.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_I) {
				useInstanced = !useInstanced;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		/*
		 * private class to handle building the shader program from filenames. This one
		 * is different from the C/C++ one in that it does not take the complete path.
		 * It has a path and a file name and then insists on the extensions .vert,
		 * .frag.
		 * 
		 * I think we will rewrite this one to do a few other things before the class is
		 * over. Right now it works.
		 */
		private class Program {

			public int name;

			public Program(GL4 gl, String root, String vertex, String fragment) {

				ShaderCode vertShader = ShaderCode.create(gl, GL_VERTEX_SHADER, this.getClass(), root, null, vertex,
						"vert", null, true);
				ShaderCode fragShader = ShaderCode.create(gl, GL_FRAGMENT_SHADER, this.getClass(), root, null, fragment,
						"frag", null, true);

				ShaderProgram shaderProgram = new ShaderProgram();

				shaderProgram.add(vertShader);
				shaderProgram.add(fragShader);

				shaderProgram.init(gl);

				name = shaderProgram.program();

				shaderProgram.link(gl, System.err);
			}
		}

		/*
		 * Class to set up debug output from OpenGL. Again, I haven't done this in the
		 * C/C++ version but it would be a good idea.
		 */
		private class GlDebugOutput implements GLDebugListener {

			private int source = 0;
			private int type = 0;
			private int id = 0;
			private int severity = 0;
			private String message = null;

			public GlDebugOutput() {
			}

			public GlDebugOutput(int source, int type, int severity) {
				this.source = source;
				this.type = type;
				this.severity = severity;
				this.message = null;
				this.id = -1;
			}

			public GlDebugOutput(String message, int id) {
				this.source = -1;
				this.type = -1;
				this.severity = -1;
				this.message = message;
				this.id = id;
			}

			@Override
			public void messageSent(GLDebugMessage event) {

				if (event.getDbgSeverity() == GL_DEBUG_SEVERITY_LOW
						|| event.getDbgSeverity() == GL_DEBUG_SEVERITY_NOTIFICATION)
					System.out.println("GlDebugOutput.messageSent(): " + event);
				else
					System.err.println("GlDebugOutput.messageSent(): " + event);

				boolean received = false;
				if (null != message && message.equals(event.getDbgMsg()) && id == event.getDbgId())
					received = true;
				else if (0 <= source && source == event.getDbgSource() && type == event.getDbgType()
						&& severity == event.getDbgSeverity())
					received = true;
			}
		}
	}

	/**
	 * Default constructor for the class does nothing in this case. It simply gives
	 * a starting point to create an instance and then run the main program from the
	 * class.
	 */
	public Snow() {
		// TODO Auto-generated constructor stub
	}

	/**
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Snow myInstance = new Snow();
		HelloTriangleSimple example = myInstance.new HelloTriangleSimple();
		example.main(args);
	}

}
