/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.ai.tests.steer.box2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/** A steering entity for box2d physics engine.
 * 
 * @author davebaol */
public class Box2dSteeringEntity implements Steerable<Vector2> {
	TextureRegion region;
	Body body;

	float boundingRadius;
	boolean tagged;

	float maxLinearSpeed;
	float maxLinearAcceleration;
	float maxAngularSpeed;
	float maxAngularAcceleration;

	boolean independentFacing;

	protected SteeringBehavior<Vector2> steeringBehavior;

	private static final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<Vector2>(new Vector2());

	public Box2dSteeringEntity (TextureRegion region, Body body, float boundingRadius) {
        System.out.println("BoundingRadius:" + boundingRadius);
		this.region = region;
		this.body = body;
		this.boundingRadius = boundingRadius;
		this.tagged = false;
	}

	public TextureRegion getRegion () {
		return region;
	}

	public void setRegion (TextureRegion region) {
		this.region = region;
	}

	public Body getBody () {
		return body;
	}

	public void setBody (Body body) {
		this.body = body;
	}

	public boolean isIndependentFacing () {
		return independentFacing;
	}

	public void setIndependentFacing (boolean independentFacing) {
		this.independentFacing = independentFacing;
	}

	@Override
	public Vector2 getPosition () {

        Vector2 pos = body.getPosition();
        pos.set(Box2dSteeringTest.metersToPixels(pos.x), Box2dSteeringTest.metersToPixels(pos.y));
        return pos;
	}

	@Override
	public float getOrientation () {
		return body.getAngle();
	}

	@Override
	public Vector2 getLinearVelocity () {
		return body.getLinearVelocity();
	}

	@Override
	public float getAngularVelocity () {
		return body.getAngularVelocity();
	}

	@Override
	public float getBoundingRadius () {
		return boundingRadius;
	}

	@Override
	public boolean isTagged () {
		return tagged;
	}

	@Override
	public void setTagged (boolean tagged) {
		this.tagged = tagged;
	}

	@Override
	public Vector2 newVector () {
		return new Vector2();
	}

	@Override
	public float vectorToAngle (Vector2 vector) {
		return (float)Math.atan2(-vector.x, vector.y);
	}

	@Override
	public Vector2 angleToVector (Vector2 outVector, float angle) {
		outVector.x = -(float)Math.sin(angle);
		outVector.y = (float)Math.cos(angle);
		return outVector;
	}

	public SteeringBehavior<Vector2> getSteeringBehavior () {
		return steeringBehavior;
	}

	public void setSteeringBehavior (SteeringBehavior<Vector2> steeringBehavior) {
		this.steeringBehavior = steeringBehavior;
	}

	public void update () {
		if (steeringBehavior != null) {
			// Calculate steering acceleration
			steeringBehavior.steer(steeringOutput);

			// Apply steering accelerations (if any)
			boolean anyAccelerations = false;
			if (!steeringOutput.linear.isZero()) {
                Vector2 force = steeringOutput.linear.scl(Gdx.graphics.getDeltaTime());
//                System.out.println("FORCE:" + force);
				body.applyForceToCenter(force, false);
				anyAccelerations = true;
			}

			if (steeringOutput.angular != 0) {
				//System.out.println("applyTorque " + steeringOutput.angular + "; body.getAngle = "+body.getAngle()+"; isFixedRoration = "+body.isFixedRotation());
				body.applyTorque(steeringOutput.angular * Gdx.graphics.getDeltaTime(), false);
				anyAccelerations = true;
			}


			if (anyAccelerations) {
// body.activate();

				// TODO:
				// Looks like truncating speeds here after applying forces doesn't work as expected.
				// We should likely cap speeds form inside an InternalTickCallback, see
				// http://www.bulletphysics.org/mediawiki-1.5.8/index.php/Simulation_Tick_Callbacks

				// Cap the linear speed
				Vector2 velocity = body.getLinearVelocity();
				float currentSpeedSquare = velocity.len2();
				float maxLinearSpeed = getMaxLinearSpeed();
				if (currentSpeedSquare > maxLinearSpeed * maxLinearSpeed) {
					body.setLinearVelocity(velocity.scl(maxLinearSpeed / (float)Math.sqrt(currentSpeedSquare)));
				}

				// Cap the angular speed
				float maxAngVelocity = getMaxAngularSpeed();
				if (body.getAngularVelocity() > maxAngVelocity) {
//					System.out.println("body.getAngularVelocity() = "+body.getAngularVelocity());
					body.setAngularVelocity(maxAngVelocity);
				}
			}

        }

		wrapAround(Box2dSteeringTest.pixelsToMeters(Gdx.graphics.getWidth()), Box2dSteeringTest.pixelsToMeters(Gdx.graphics.getHeight()));
	}

	// the display area is considered to wrap around from top to bottom
	// and from left to right
	protected void wrapAround (float maxX, float maxY) {
		float k = Float.POSITIVE_INFINITY;
		Vector2 pos = body.getPosition();
		
		if (pos.x > maxX) k = pos.x = 0.0f;

		if (pos.x < 0) k = pos.x = maxX;

		if (pos.y < 0) k = pos.y = maxY;

		if (pos.y > maxY) k = pos.y = 0.0f;
		
		if (k != Float.POSITIVE_INFINITY)
			body.setTransform(pos, body.getAngle());
	}

	public void draw(Batch batch) {
		Vector2 pos = body.getPosition();
        float w = boundingRadius * 2.0f;
        float h = boundingRadius * 2.0f;
		//float w = region.getRegionWidth();
		//float h = region.getRegionHeight();
		float ox = w / 2f;
		float oy = h / 2f;

		batch.draw(region, Box2dSteeringTest.metersToPixels(pos.x) - ox, Box2dSteeringTest.metersToPixels(pos.y) - oy,
			ox, oy, 
			w, h,
			1, 1, 
			body.getAngle() * MathUtils.radiansToDegrees);
	}

	//
	// Limiter implementation
	//

	@Override
	public float getMaxLinearSpeed () {
		return maxLinearSpeed;
	}

	@Override
	public void setMaxLinearSpeed (float maxLinearSpeed) {
		this.maxLinearSpeed = maxLinearSpeed;
	}

	@Override
	public float getMaxLinearAcceleration () {
		return maxLinearAcceleration;
	}

	@Override
	public void setMaxLinearAcceleration (float maxLinearAcceleration) {
		this.maxLinearAcceleration = maxLinearAcceleration;
	}

	@Override
	public float getMaxAngularSpeed () {
		return maxAngularSpeed;
	}

	@Override
	public void setMaxAngularSpeed (float maxAngularSpeed) {
		this.maxAngularSpeed = maxAngularSpeed;
	}

	@Override
	public float getMaxAngularAcceleration () {
		return maxAngularAcceleration;
	}

	@Override
	public void setMaxAngularAcceleration (float maxAngularAcceleration) {
		this.maxAngularAcceleration = maxAngularAcceleration;
	}
}
