package com.xacobeu;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;

import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import javax.swing.*;

import java.awt.*;

public class PlanetRenderer {

	private long window;

	// Screen dimensions.
	private static final int screenWidth = 512;
	private static final int screenHeight = 512;

	// Center of the screen.
	private static final int centerX = screenWidth / 2;
	private static final int centerY = screenHeight / 2;

	// Gravitational constant.
	private final double G = 6.67430e-11;

	// Planets.
	private ArrayList<Body> objects2D = new ArrayList<>();
	private ArrayList<Body> objects3D = new ArrayList<>();

	// Canvas to integrate LJWGL with Swing.
	private static boolean running = false;
	
	// Rendering mode: 0 = 2D and 1 = 3D.
	private int renderingMode = 0;
		
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

		window = glfwCreateWindow(screenWidth, screenHeight, "Planet simulation", NULL, NULL);
		if ( window == NULL ) {
			throw new RuntimeException("Failed to create the GLFW window");
		} else {
			System.out.println("Window created");
		}

		// Center the window
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(
			window,
			(vidmode.width() - screenWidth) / 2,
			(vidmode.height() - screenHeight) / 2
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
			glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

		}
		else if (renderingMode == 1) {
			
			System.out.println("3D rendering mode");

			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);

			float fov = 45.0f;  // Field of view
			float aspect = (float) screenWidth / (float) screenHeight;
			float zNear = 0.1f;
			float zFar = 100.0f;
	
			float top = (float) Math.tan(Math.toRadians(fov / 2.0)) * zNear;
			float right = top * aspect;

			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glFrustum(-right, right, -top, top, zNear, zFar);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

		}

		// Set the clear color
		glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		System.out.println("init complete");
	}

	private void render() {

		System.out.println("Starting rendering loop");
		// Run until escape key is pressed.
		while (running) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

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
					// TODO: Fix Trail drawing.
					// p1.drawTrail();
					p1.draw();
					((Planet2D) p1).checkBorderCollision(screenWidth, screenHeight);
					
				}

			} else if (renderingMode == 1) {
				// Update 3D physics.
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
					// TODO: Fix Trail drawing.
					// p1.drawTrail();
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
		objects3D.add(new Planet3D(centerX, centerY, 0, 20, 1.98e30, Colors.YELLOW));
		objects3D.add(new Planet3D(centerX, centerY + 100, 0, 10, 5.97e24, Colors.BLUE));

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
