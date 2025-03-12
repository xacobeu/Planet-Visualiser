package com.xacobeu;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;

import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import javax.swing.*;

import java.awt.*;
import java.nio.FloatBuffer;

public class PlanetRenderer {

	// Window to render in.
	private long window;

	// Screen dimensions.
	private static final int WIDTH = 1024;
	private static final int HEIGHT = 1024;

	// Center of the screen.
	private static final int centerX = WIDTH / 2;
	private static final int centerY = HEIGHT / 2;

	// Gravitational constant.
	private final double G = 6.67430e-11;

	// Planets.
	private ArrayList<Body> objects2D = new ArrayList<>();
	private ArrayList<Body> objects3D = new ArrayList<>();

	// Canvas to integrate LJWGL with Swing.
	private static boolean running = false;
	
	// Rendering mode: 0 = 2D and 1 = 3D.
	private int renderingMode = 0;

    // Camera variables
    private float yaw = -90.0f;   // Yaw angle (left/right rotation)
    private float pitch = 0.0f;   // Pitch angle (up/down rotation)
    private float lastX = WIDTH / 2.0f; // Last mouse X position
    private float lastY = HEIGHT / 2.0f; // Last mouse Y position
    private boolean firstMouse = true; // Flag to handle initial mouse movement
	private float cameraSpeed = 1.0f;
	private boolean fastCamera = false;

	// Camera position
    private float cameraX = 0.0f;
    private float cameraY = 0.0f;
    private float cameraZ = 5.0f;
		
	// Camera front direction
    private float frontX = 0.0f;
    private float frontY = 0.0f;
    private float frontZ = -1.0f;

    // Camera right direction
    private float rightX = 1.0f;
    private float rightY = 0.0f;
    private float rightZ = 0.0f;

    // Up vector
    private final float upX = 0.0f;
    private final float upY = 1.0f;
    private final float upZ = 0.0f;

	private boolean[] keyStates = new boolean[GLFW_KEY_LAST + 1];

	public PlanetRenderer() {
		initialiseObjects();
	}

	public void start() {
		System.out.println("Starting simulation");
		if (running) return;
		running = true;
		new Thread(this::run).start();
	}

	public void stop() {
		System.out.println("Stopping simulation");
		running = false;
	}

	public void reset() {
		System.out.println("Resetting simulation");
		objects2D.clear();
		objects3D.clear();
		initialiseObjects();
	}

	public void run() {
		init();
		render();
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private void init() {

		System.out.println("Initialising simulation");

		glfwInit();
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(WIDTH, HEIGHT, "Planet simulation", NULL, NULL);
		if ( window == NULL ) {
			throw new RuntimeException("Failed to create the GLFW window");
		} else {
			System.out.println("Window created");
		}

		// Center the window
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(
			window,
			(vidmode.width() - WIDTH) / 2,
			(vidmode.height() - HEIGHT) / 2
		);

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1); // Enable v-sync.
		glfwShowWindow(window);

		// Set up OpenGL context.
		GL.createCapabilities();

		System.out.println("OpenGL " + glGetString(GL_VERSION) + " initialised.");

		if (renderingMode == 0) {
			System.out.println("2D rendering mode");

			// Make coordinate system match the window size.
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

		}
		else if (renderingMode == 1) {
			
			System.out.println("3D rendering mode");

			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);

			glEnable(GL_LIGHTING);
			glEnable(GL_LIGHT0);
			glEnable(GL_NORMALIZE);

			float[] lightPosition = {0.0f, 0.0f, 0.0f, 1.0f}; // Position of the Sun (x, y, z, w)
			float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f}; // Ambient light (gray)
			float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f}; // Diffuse light (white)
			float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // Specular light (white)

			// Set light properties
			glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
			glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
			glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
			glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular);


			// Set up the projection matrix
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			float aspect = (float) WIDTH / HEIGHT;
			float fov = 90.0f; // Field of view
			float zNear = 0.1f; // Near clipping plane
			float zFar = 100000.0f; // Far clipping plane
			float top = (float) Math.tan(Math.toRadians(fov / 2.0)) * zNear;
			float right = top * aspect;
			glFrustum(-right, right, -top, top, zNear, zFar);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			// Set up camera rotation.
			glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
				@Override
				public void invoke(long window, double xpos, double ypos) {
					handleMouseInput(xpos, ypos);
				}
			});

			// Set up camera movement.
			glfwSetKeyCallback(window, new GLFWKeyCallback() {
				@Override
				public void invoke(long window, int key, int scancode, int action, int mods) {
					if (key >= 0 && key < keyStates.length) {
						if (action == GLFW_PRESS) {
							keyStates[key] = true; // Key is pressed
						} else if (action == GLFW_RELEASE) {
							keyStates[key] = false; // Key is released
						}
					}
				}
			});

			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		}

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.1f, 1.0f);
		System.out.println("init complete");
	}

	private void handleMouseInput(double xpos, double ypos) {
        if (firstMouse) {
            lastX = (float) xpos;
            lastY = (float) ypos;
            firstMouse = false;
        }

        float xoffset = (float) xpos - lastX;
        float yoffset = lastY - (float) ypos; // Reversed since y-coordinates go from bottom to top
        lastX = (float) xpos;
        lastY = (float) ypos;

        float sensitivity = 0.1f; // Adjust sensitivity
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += xoffset;
        pitch += yoffset;

        // Constrain pitch to avoid flipping
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        // Calculate the new front vector
        frontX = (float) (Math.cos(Math.toRadians(yaw)) * (float) (Math.cos(Math.toRadians(pitch))));
        frontY = (float) Math.sin(Math.toRadians(pitch));
        frontZ = (float) (Math.sin(Math.toRadians(yaw)) * (float) (Math.cos(Math.toRadians(pitch))));

        // Normalize the front vector
        float frontLength = (float) Math.sqrt(frontX * frontX + frontY * frontY + frontZ * frontZ);
        frontX /= frontLength;
        frontY /= frontLength;
        frontZ /= frontLength;

        // Calculate the right vector
        rightX = frontY * upZ - frontZ * upY;
        rightY = frontZ * upX - frontX * upZ;
        rightZ = frontX * upY - frontY * upX;

        // Normalize the right vector
        float rightLength = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        rightX /= rightLength;
        rightY /= rightLength;
        rightZ /= rightLength;
    }

	private FloatBuffer createViewMatrix() {
        // Create the view matrix
        FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
        
		// Calculate the camera's look-at point
		float lookAtX = cameraX + frontX;
		float lookAtY = cameraY + frontY;
		float lookAtZ = cameraZ + frontZ;

		// Calculate the view matrix using the camera's position, look-at point, and up vector
		float[] viewMatrixArray = calculateLookAtMatrix(cameraX, cameraY, cameraZ, lookAtX, lookAtY, lookAtZ, upX, upY, upZ);
		viewMatrix.put(viewMatrixArray).flip();

		return viewMatrix;
    }

	private float[] calculateLookAtMatrix(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
		// Calculate the forward, right, and up vectors
		float[] forward = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
		float forwardLength = (float) Math.sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2]);
		forward[0] /= forwardLength;
		forward[1] /= forwardLength;
		forward[2] /= forwardLength;

		float[] up = {upX, upY, upZ};
		float[] right = {
			forward[1] * up[2] - forward[2] * up[1],
			forward[2] * up[0] - forward[0] * up[2],
			forward[0] * up[1] - forward[1] * up[0]
		};
		float rightLength = (float) Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
		right[0] /= rightLength;
		right[1] /= rightLength;
		right[2] /= rightLength;

		float[] newUp = {
			right[1] * forward[2] - right[2] * forward[1],
			right[2] * forward[0] - right[0] * forward[2],
			right[0] * forward[1] - right[1] * forward[0]
		};

		// Create the view matrix
		return new float[]{
			right[0], newUp[0], -forward[0], 0.0f,
			right[1], newUp[1], -forward[1], 0.0f,
			right[2], newUp[2], -forward[2], 0.0f,
			-(right[0] * eyeX + right[1] * eyeY + right[2] * eyeZ),
			-(newUp[0] * eyeX + newUp[1] * eyeY + newUp[2] * eyeZ),
			forward[0] * eyeX + forward[1] * eyeY + forward[2] * eyeZ,
			1.0f
		};
	}

	private void handleKeyboardInput() {
		if (keyStates[GLFW_KEY_W]) { // Move forward
			cameraX += frontX * cameraSpeed;
			cameraY += frontY * cameraSpeed;
			cameraZ += frontZ * cameraSpeed;
		}
		if (keyStates[GLFW_KEY_S]) { // Move backward
			cameraX -= frontX * cameraSpeed;
			cameraY -= frontY * cameraSpeed;
			cameraZ -= frontZ * cameraSpeed;
		}
		if (keyStates[GLFW_KEY_A]) { // Move left
			cameraX -= rightX * cameraSpeed;
			cameraY -= rightY * cameraSpeed;
			cameraZ -= rightZ * cameraSpeed;
		}
		if (keyStates[GLFW_KEY_D]) { // Move right
			cameraX += rightX * cameraSpeed;
			cameraY += rightY * cameraSpeed;
			cameraZ += rightZ * cameraSpeed;
		}
		if (keyStates[GLFW_KEY_SPACE]) { // Move up
			cameraY += cameraSpeed;
		}
		if (keyStates[GLFW_KEY_LEFT_SHIFT]) { // Move down
			cameraY -= cameraSpeed;
		}
		if (keyStates[GLFW_KEY_R]) { // Close the window
			if (fastCamera) {
				cameraSpeed = 1.0f;
				fastCamera = false;
			} else {
				cameraSpeed = 15.0f;
				fastCamera = true;
				
			}
		}
	}

	private void render() {

		System.out.println("Starting rendering loop");

		float[] lightPosition = {0.0f, 0.0f, 0.0f, 1.0f}; // Sun's position (x, y, z, w)
		glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);

		// Run until escape key is pressed.
		while (running) {

			handleKeyboardInput();

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glLoadIdentity();

			if (renderingMode == 0) {
				// Update 2D physics.
				for (Body p1 : objects2D) {
					for (Body p2 : objects2D) {
						if (p1 == p2) continue;

						double dx = ((Planet2D) p2).getPositionX() - ((Planet2D) p1).getPositionX();
						double dy = ((Planet2D) p2).getPositionY() - ((Planet2D) p1).getPositionY();

						double distance = Math.sqrt(dx * dx + dy * dy);
						if (distance <= ((Planet2D) p1).getRadius() + ((Planet2D) p2).getRadius()) {
							p1.resolveCollision(p2);
							continue;
						}
						distance *= 6e5;

						double directionX = dx / distance;
						double directionY = dy / distance;

						double force = G * ((Planet2D) p1).getMass() * ((Planet2D) p2).getMass() / (distance * distance);
						double acc = force / ((Planet2D) p1).getMass();
						((Planet2D) p1).setVelocityX(((Planet2D) p1).getVelocityX() + acc * directionX);
						((Planet2D) p1).setVelocityY(((Planet2D) p1).getVelocityY() + acc * directionY);
					}

					p1.updatePosition();
					p1.drawTrail();
					p1.draw();
					((Planet2D) p1).checkBorderCollision(WIDTH, HEIGHT);
				}

			} else if (renderingMode == 1) {

				// Set the modelview matrix
				glMatrixMode(GL_MODELVIEW);
				glLoadIdentity();
	
				// Load the view matrix
				FloatBuffer viewMatrix = createViewMatrix();
				glLoadMatrixf(viewMatrix);

				// // Update 3D physics.
				for (Body p1 : objects3D) {
					for (Body p2 : objects3D) {
						if (p1 == p2) continue;

						double dx = ((Planet3D) p2).getPositionX() - ((Planet3D) p1).getPositionX();
						double dy = ((Planet3D) p2).getPositionY() - ((Planet3D) p1).getPositionY();
						double dz = ((Planet3D) p2).getPositionZ() - ((Planet3D) p1).getPositionZ();

						double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
						if (distance <= ((Planet3D) p1).getRadius() + ((Planet3D) p2).getRadius()) {
							p1.resolveCollision(p2);
							continue;
						}
						distance *= 6e5;

						double directionX = dx / distance;
						double directionY = dy / distance;
						double directionZ = dz / distance;

						double force = G * ((Planet3D) p1).getMass() * ((Planet3D) p2).getMass() / (distance * distance);
						double acc = force / ((Planet3D) p1).getMass();
						((Planet3D) p1).setVelocityX(((Planet3D) p1).getVelocityX() + acc * directionX);
						((Planet3D) p1).setVelocityY(((Planet3D) p1).getVelocityY() + acc * directionY);
						((Planet3D) p1).setVelocityZ(((Planet3D) p1).getVelocityZ() + acc * directionZ);
					}
					p1.updatePosition();
					p1.drawTrail();
					p1.draw();
				}
			}

			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}

	public void initialiseObjects() {
		// Earth and Sun.
		objects2D.add(new Planet2D(centerX, centerY, 20, 1.98e30, Colors.YELLOW));
		objects2D.add(new Planet2D(centerX, centerY + 100, 10, 5.97e24, Colors.BLUE));
		objects2D.add(new Planet2D(centerX, centerY + 200, 10, 5.97e24, Colors.GREEN));
		objects2D.get(1).setVelocityX(2);
		objects2D.get(2).setVelocityX(1);

		// Stable orbit.
		// planets.add(new Planet(100, 100, 10, 1e20, Colors.PURPLE));
		// planets.add(new Planet(200, 100, 10, 1e20, Colors.RED));
		// planets.add(new Planet(centerX, centerY, 20, 1e30, Colors.ORANGE));
		// planets.get(1).setVelocityX(2);
		// planets.get(2).setVelocityX(1);

		// Random planets.
		// Random random = new Random();
		// for (int i = 0; i < 100; i++) {
		// 	planets.add(new Planet(random.nextInt(screenWidth), random.nextInt(screenHeight), 5, 1e30, Colors.WHITE));
		// }

		// 3D planets.
		objects3D.add(new Planet3D(0, 0, 0, 20, 1.98e30, Colors.YELLOW));
		objects3D.add(new Planet3D(0, 100, 0, 10, 5.97e24, Colors.GREEN));
		objects3D.add(new Planet3D(100, 0, 100, 10, 5.97e24, Colors.DARK_GRAY));

		objects3D.get(1).setVelocityX(2);
		objects3D.get(2).setVelocityY(2);

		// Scaled down real solar system.
		// Sun
		// objects3D.add(new Planet3D(57.91, 0, 0, 124, 3.30e23, Colors.GRAY)); // Mercury
		// objects3D.add(new Planet3D(108.2, 0, 0, 0.161, 4.87e24, Colors.ORANGE)); // Venus
		// objects3D.add(new Planet3D(149.6, 0, 0, 0.164, 5.97e24, Colors.BLUE)); // Earth
		// objects3D.add(new Planet3D(227.94, 0, 0, 0.034, 6.42e23, Colors.RED)); // Mars
		// objects3D.add(new Planet3D(778.33, 0, 0, 0.699, 1.90e27, Colors.BROWN)); // Jupiter
		// objects3D.add(new Planet3D(1429.4, 0, 0, 0.582, 5.68e26, Colors.YELLOW)); // Saturn
		// objects3D.add(new Planet3D(2870.99, 0, 0, 0.254, 8.68e25, Colors.CYAN)); // Uranus
		// objects3D.add(new Planet3D(4504.3, 0, 0, 0.246, 1.02e26, Colors.BLUE)); // Neptune
		
		// // Set initial velocities for planets (approximate values for circular orbits)
		// for (Body planet : objects3D) {
		// 	if (planet instanceof Planet3D) {
		// 		Planet3D p = (Planet3D) planet;
		// 		p.setVelocityY(1.5); // Set initial velocity for orbit
		// 	}
		// }

		// objects3D.add(new Planet3D(0, 0, 0, 6.96, 1.98e30, Colors.YELLOW)); // Sun

	}

	public void setMode(int mode) {
		if (mode == 0) renderingMode = 0;
		else renderingMode = 1;
	}
	
	public static void main(String[] args) {

		UIManager.put("ToggleButton.select", new Color(0, 0, 225));

		JFrame frame = new JFrame("Planet simulation");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(true);

		JPanel panel = new JPanel();
		JButton startButton = new JButton("Start");
		JButton stopButton = new JButton("Stop");
		JButton resetButton = new JButton("Reset");
		JToggleButton mode2D = new JToggleButton("2D");
		JToggleButton mode3D = new JToggleButton("3D");

		ButtonGroup modes = new ButtonGroup();
		modes.add(mode2D);
		modes.add(mode3D);

		// Make stuff beautiful.
		panel.setBackground(new Color(0, 0, 51));

		startButton.setPreferredSize(new Dimension(100, 50));
		startButton.setBorderPainted(false);
		startButton.setFocusable(false);
		startButton.setBackground(new Color(0, 0, 102));
		startButton.setForeground(Color.WHITE);
		
		stopButton.setPreferredSize(new Dimension(100, 50));
		stopButton.setBorderPainted(false);
		stopButton.setFocusable(false);
		stopButton.setBackground(new Color(0, 0, 102));
		stopButton.setForeground(Color.WHITE);

		resetButton.setPreferredSize(new Dimension(100, 50));
		resetButton.setBorderPainted(false);
		resetButton.setFocusable(false);
		resetButton.setBackground(new Color(0, 0, 102));
		resetButton.setForeground(Color.WHITE);

		mode2D.setPreferredSize(new Dimension(100, 50));
		mode2D.setFocusable(false);
		mode2D.setBorderPainted(false);
		mode2D.setBackground(new Color(0, 0, 160));
		mode2D.setForeground(Color.BLACK);

		mode3D.setPreferredSize(new Dimension(100, 50));
		mode3D.setFocusable(false);
		mode3D.setBorderPainted(false);
		mode3D.setBackground(new Color(0, 0, 160));
		mode3D.setForeground(Color.BLACK);

		panel.add(startButton);
		panel.add(stopButton);
		panel.add(resetButton);
		panel.add(mode2D);
		panel.add(mode3D);

		frame.add(panel);

		PlanetRenderer renderer = new PlanetRenderer();

		startButton.addActionListener(e -> {
			renderer.start();
			mode2D.setEnabled(false);
			mode3D.setEnabled(false);
		});
		stopButton.addActionListener(e -> {
			renderer.stop();
			mode2D.setEnabled(true);
			mode3D.setEnabled(true);
		});
		resetButton.addActionListener(e -> renderer.reset());

		mode2D.setSelected(true);
		mode2D.addActionListener(e -> {
			if (mode2D.isSelected()) {
				mode3D.setSelected(false);
				renderer.setMode(0);
			}
		});
		mode3D.addActionListener(e -> {
			if (mode3D.isSelected()) {
				mode2D.setSelected(false);
				renderer.setMode(1);
			}
		});
		
		frame.pack();
		frame.setVisible(true);
	}
}
