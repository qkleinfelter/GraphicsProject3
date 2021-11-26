/**
 * ParticleInterface
 * @author Jerry Heuring
 * 
 * This is my basic interface for a particle.  One does not need to 
 * actually use all of the pieces but I want the placeholders here..
 */
public interface ParticleInterface {
	void init(int maxParticles);
	void generate(int maxNewParticles);
	void update(float timeStep);
	void compact();
	float[] getPositions();
	float[] getVelocities();
	float[]  getAccelerations();
	float[] getOrientations();
	int getNumberOfParticles();
}
