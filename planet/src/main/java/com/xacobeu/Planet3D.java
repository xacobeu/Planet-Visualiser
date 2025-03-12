package com.xacobeu;

import static org.lwjgl.opengl.GL11.*;

public class Planet3D extends Planet2D {
    private double positionZ;
    private double velocityZ;

    public Planet3D(double positionX, double positionY, double positionZ, double radius, double mass, float[] color) {
        super(positionX, positionY, radius, mass, color);
        this.positionZ = positionZ;
        this.velocityZ = 0;
    }

    @Override
    public void resolveCollision(Body p2) {
		double dx = p2.getPositionX() - positionX;
		double dy = p2.getPositionY() - positionY;
        double dz = p2.getPositionZ() - positionZ;
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
	
		// Normalize the collision normal
		double nx = dx / distance;
		double ny = dy / distance;
        double nz = dz / distance;
	
		// Relative velocity
		double vx = p2.getVelocityX() - velocityX;
		double vy = p2.getVelocityY() - velocityY;
        double vz = p2.getVelocityZ() - velocityZ;
		
		// Velocity along the normal
		double vn = vx * nx + vy * ny + vz * nz;
		
		if (vn > 0) return; // They are moving apart, no need to resolve
	
		// Compute impulse scalar
		double massSum = mass + p2.getMass();
		double impulse = (2 * vn) / massSum;
	
		// Apply impulse
		setVelocityX(velocityX + impulse * p2.getMass() * nx);
		setVelocityY(velocityY + impulse * p2.getMass() * ny);
        setVelocityZ(velocityZ + impulse * p2.getMass() * nz);
		p2.setVelocityX(p2.getVelocityX() - impulse * mass * nx);
		p2.setVelocityY(p2.getVelocityY() - impulse * mass * ny);
        p2.setVelocityZ(p2.getVelocityZ() - impulse * mass * nz);
	}

    @Override
    public void updatePosition() {
        super.updatePosition();
        positionZ += velocityZ;

        // Store past positions
        if (trail.size() >= TRAIL_LENGTH) {
            trail.poll(); // Remove oldest position
        }
        trail.add(new double[]{positionX, positionY, positionZ});
    }

    @Override
    public void drawTrail() {
        if (trail.isEmpty() || trail == null) return;
        
        glColor4f(Colors.WHITE[0], Colors.WHITE[1], Colors.WHITE[2], 1.0f);

        glBegin(GL_POINTS);
        for (double[] pos : trail) {
            if (pos != null && pos.length == 3) {
                glVertex3d(pos[0], pos[1], pos[2]);
            }
        }
        glEnd();
    }

    @Override
    public void draw() {
        glPushMatrix();
        
        // Translate to the planet's position
        glTranslatef((float) positionX, (float) positionY, (float) positionZ);
        
        
        // Set material properties
        float[] materialAmbient = {0.2f, 0.2f, 0.2f, 1.0f}; // Ambient reflection
        float[] materialDiffuse = {color[0], color[1], color[2], 1.0f}; // Diffuse reflection (use planet's color)
        float[] materialSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // Specular reflection (white)
        float materialShininess = 50.0f; // Shininess (higher values = smaller, sharper highlights)

        glMaterialfv(GL_FRONT, GL_AMBIENT, materialAmbient);
        glMaterialfv(GL_FRONT, GL_DIFFUSE, materialDiffuse);
        glMaterialfv(GL_FRONT, GL_SPECULAR, materialSpecular);
        glMaterialf(GL_FRONT, GL_SHININESS, materialShininess);

        // Set the planet's color
        // glColor3f(color[0], color[1], color[2]);
    
        // Draw the sphere
        for (int i = 0; i <= resolution; i++) {
            double lat0 = Math.PI * (-0.5 + (double) (i - 1) / resolution);
            double z0 = Math.sin(lat0);
            double zr0 = Math.cos(lat0);
    
            double lat1 = Math.PI * (-0.5 + (double) i / resolution);
            double z1 = Math.sin(lat1);
            double zr1 = Math.cos(lat1);
    
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= resolution; j++) {
                double lng = 2 * Math.PI * (double) (j - 1) / resolution;
                double x = Math.cos(lng);
                double y = Math.sin(lng);
    
                glNormal3f((float) (x * zr0), (float) (y * zr0), (float) (z0));
                glVertex3f((float) (radius * x * zr0), (float) (radius * y * zr0), (float) (radius * z0));
                glNormal3f((float) (x * zr1), (float) (y * zr1), (float) (z1));
                glVertex3f((float) (radius * x * zr1), (float) (radius * y * zr1), (float) (radius * z1));
            }
            glEnd();
        }
    
        glPopMatrix();
    }

    public void applyVelocity(double velocityX, double velocityY, double velocityZ) {
        super.applyVelocity(velocityX, velocityY);
        this.velocityZ += velocityZ;
    }

    public void checkBorderCollision(int screenWidth, int screenHeight, int screenDepth) {
        super.checkBorderCollision(screenWidth, screenHeight);
        if (positionZ + radius > screenDepth || positionZ - radius < 0) {
            velocityZ = -velocityZ;
        }
    }

    public double getPositionZ() {
        return positionZ;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public void setPositionZ(double positionZ) {
        this.positionZ = positionZ;
    }

    public void setVelocityZ(double velocityZ) {
        this.velocityZ = velocityZ;
    }
    
}
