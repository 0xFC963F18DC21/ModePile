package zeroxfc.nullpo.custom.libs.particles;

import mu.nu.nullpo.game.event.EventReceiver;
import zeroxfc.nullpo.custom.libs.DoubleVector;
import zeroxfc.nullpo.custom.libs.Interpolation;
import zeroxfc.nullpo.custom.libs.PrimitiveDrawingHook;

public class Particle {
	/** Default colour */
	private static final int[] DEFAULT_COLOUR = { 255, 255, 255, 255 };

	/** Particle shape */
	private ParticleShape shape;

	/** Lifetime */
	private final int particleMaxLifetime;

	/** Current life */
	private int particleLifetime;

	/** Position vector */
	private DoubleVector position;

	/** Velocity vector */
	private DoubleVector velocity;

	/** Acceleration vector */
	private DoubleVector acceleration;

	/*
	 * Colour variables.
	 * Please use <code>0 <= value <= 255</code>
	 */

	/** X size */
	private int sizeX;

	/** Y size */
	private int sizeY;

	/** Red colour component */
	private int red;

	/** Green colour component */
	private int green;

	/** Blue colour component */
	private int blue;

	/** Alpha component */
	private int alpha;

	/** Red colour component at end */
	private int redEnd;

	/** Green colour component at end */
	private int greenEnd;

	/** Blue colour component at end */
	private int blueEnd;

	/** Alpha component at end */
	private int alphaEnd;

	/**
	 * Create an instance of a particle.
	 * @param shape The shape of the particle. Warning: SDL cannot draw circular particles.
	 * @param maxLifeTime The maximum frame lifetime of the particle.
	 * @param position Vector position of the particle.
	 * @param velocity Vector velocity of the particle.
	 * @param acceleration Vector acceleration of the particle.
	 * @param sizeX Horizontal size of the particle.
	 * @param sizeY Vertical size of the particle.
	 * @param red Red component of colour.
	 * @param green Green component of colour.
	 * @param blue Blue component of colour.
	 * @param alpha Alpha component of colour.
	 * @param redEnd Red component of colour at particle death.
	 * @param greenEnd Green component of colour at particle death.
	 * @param blueEnd Blue component of colour at particle death.
	 * @param alphaEnd Alpha component of colour at particle death.
	 */
	public Particle(ParticleShape shape, int maxLifeTime, DoubleVector position, DoubleVector velocity,
	                DoubleVector acceleration, int sizeX, int sizeY, int red, int green, int blue, int alpha,
	                int redEnd, int greenEnd, int blueEnd, int alphaEnd) {
	    this.shape = shape;
	    particleMaxLifetime = maxLifeTime;
	    this.position = position;
	    this.velocity = velocity;
	    this.acceleration = acceleration;
	    this.sizeX = sizeX;
	    this.sizeY = sizeY;
	    this.red = red;
	    this.green = green;
	    this.blue = blue;
	    this.alpha = alpha;
		this.redEnd = redEnd;
		this.greenEnd = greenEnd;
		this.blueEnd = blueEnd;
		this.alphaEnd = alphaEnd;

	    particleLifetime = 0;
	}

	/**
	 * Draw the particle.
	 * @param receiver Renderer to draw with.
	 */
	public void draw(EventReceiver receiver) {
		int ur, ug, ub, ua;
		ur = Interpolation.lerp(red, redEnd, (double)particleLifetime / particleMaxLifetime);
		ug = Interpolation.lerp(green, greenEnd, (double)particleLifetime / particleMaxLifetime);
		ub = Interpolation.lerp(blue, blueEnd, (double)particleLifetime / particleMaxLifetime);
		ua = Interpolation.lerp(alpha, alphaEnd, (double)particleLifetime / particleMaxLifetime);

		switch (shape) {
			case Rectangle:
				PrimitiveDrawingHook.drawRectangle(receiver, (int)position.getX() - (sizeX / 2), (int)position.getY() - (sizeY / 2), sizeX, sizeY, ur, ug, ub, ua, true);
				break;
			case Circle:
				PrimitiveDrawingHook.drawOval(receiver, (int)position.getX() - (sizeX / 2), (int)position.getY() - (sizeY / 2), sizeX, sizeY, ur, ug, ub, ua, true);
				break;
			default:
				break;
		}
	}

	public boolean update() {
		velocity = DoubleVector.add(velocity, acceleration);
		position = DoubleVector.add(position, velocity);

		return ++particleLifetime > particleMaxLifetime;
	}

	/**
	 * Particle Shapes
	 * Warning: you cannot use circular particles with SDL.
	 */
	public enum ParticleShape {
		Rectangle, Circle
	}
}
