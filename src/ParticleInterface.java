/**
 * ParticleInterface
 * @author Jerry Heuring
 * 
 * This is my basic interface for a particle.  One does not need to 
 * actually use all of the pieces but I want the placeholders here..
 */
public interface ParticleInterface {
	public void init(int maxParticles);
	public void generate(int maxNewParticles);
	public void update(float timeStep);
	public void compact();
	public float[] getPositions();
	public float[] getVelocities();
	public float[]  getAccelerations();
	public float[] getOrientations();
	public int getNumberOfParticles();
}
