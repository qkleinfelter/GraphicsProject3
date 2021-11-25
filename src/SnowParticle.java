public class SnowParticle implements ParticleInterface {

    int numberOfParticles;
    int maxParticles;
    float[] positions;
    float[] velocities;
    float[] accelerations;
    float[] orientations;


    @Override
    public void init(int maxParticles) {
        numberOfParticles = 0;
        this.maxParticles = maxParticles;
        positions = new float[maxParticles * 3];
        velocities = new float[maxParticles * 3];
        accelerations = new float[maxParticles * 3];
        orientations = new float[maxParticles * 4];
    }

    @Override
    public void generate(int maxNewParticles) {
        if (maxNewParticles + numberOfParticles < maxParticles) {
            for (int currentParticle = 0; currentParticle < maxNewParticles; currentParticle++) {
                positions[(numberOfParticles + currentParticle) * 3] = 0;
                positions[(numberOfParticles + currentParticle) * 3 + 1] = 0;
                positions[(numberOfParticles + currentParticle) * 3 + 2] = 0;
                double angle = Math.random() * 2 * Math.PI - Math.PI;
                double initialUpward = Math.random() * 5 + 32;

                velocities[(numberOfParticles + currentParticle) * 3] = (float) (Math.cos(angle));
                velocities[(numberOfParticles + currentParticle) * 3 + 1] = (float) initialUpward;
                velocities[(numberOfParticles + currentParticle) * 3 + 2] = (float) (Math.sin(angle));

                accelerations[(numberOfParticles + currentParticle) * 3] = 0;
                accelerations[(numberOfParticles + currentParticle) * 3 + 1] = -32.2f;
                accelerations[(numberOfParticles + currentParticle) * 3 + 2] = 0;

                orientations[(numberOfParticles + currentParticle) * 4] = 1;
                orientations[(numberOfParticles + currentParticle) * 4 + 1] = 0;
                orientations[(numberOfParticles + currentParticle) * 4 + 2] = 0;
                orientations[(numberOfParticles + currentParticle) * 4 + 3] = 0;
            }

            numberOfParticles += maxNewParticles;
        }
    }

    @Override
    public void update(float timeStep) {
        float[] newPosition = new float[3];
        float[] newVelocity = new float[3];
        float[] newAcceleration = new float[3];
        float[] newOrientation = new float[4];

        for (int current = 0; current < numberOfParticles * 3; current++) {
            for (int i = 0; i < 3; i++) {
                newPosition[i] = positions[current + i] + velocities[current + i] * timeStep + accelerations[current + i] * timeStep * timeStep / 2.0f;
            }
            for (int i = 0; i < 3; i++) {
                newVelocity[i] = velocities[current + i] + accelerations[current + i] * timeStep;
            }
            for (int i = 0; i < 3; i++) {
                positions[current + i] = newPosition[i];
                velocities[current + i] = newVelocity[i];
            }
        }
    }

    @Override
    public void compact() {
        int current, i;

        for (current = 0; current < numberOfParticles; ++current) {
            while (positions[current * 3 + 1] < 0.0f && current < numberOfParticles) {
                for (i = 0; i < 3; ++i) {
                    positions[current * 3 + i] = positions[(numberOfParticles - 1) * 3 + i];
                    velocities[current * 3 + i] = velocities[(numberOfParticles - 1) * 3 + i];
                    accelerations[current * 3 + i] = accelerations[(numberOfParticles - 1) * 3 + i];
                }
                for (i = 0; i < 4; ++i) {
                    orientations[current * 4 + i] = orientations[(numberOfParticles - 1) * 4 + i];
                }
                --numberOfParticles;
            }
        }
    }

    @Override
    public float[] getPositions() {
        return positions;
    }

    @Override
    public float[] getVelocities() {
        return velocities;
    }

    @Override
    public float[] getAccelerations() {
        return accelerations;
    }

    @Override
    public float[] getOrientations() {
        return orientations;
    }

    @Override
    public int getNumberOfParticles() {
        return numberOfParticles;
    }
}
