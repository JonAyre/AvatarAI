package com.avatarai.utils;

import java.util.HashMap;

public class Entity {
	private String type;
	private final String id;

	HashMap<String, Feature> features;

	public Entity(String newType, String newId) {
		id = newId;
		type = newType;
		features = new HashMap<>();
	}

	public void addFeature(String id, Feature feature) {
		features.put(id, feature);
	}

	public Feature getFeature(String id) {
		return features.get(id);
	}

	// Compare the corresponding features of this entity with another provided entity.
	// 1 = the same, 0 = no correlation, -1 = diametrically opposed
	// (Only compares features that exist in both entities)
	double compare(Entity otherEntity) {
		double match = 0.0;
		Feature thisFeature, otherFeature;
		int count = 0;

		for (String id : features.keySet()) {
			thisFeature = features.get(id);
			otherFeature = otherEntity.getFeature(id);
			if (otherEntity.getFeature(id) != null) {
				match += thisFeature.compare(otherFeature);
				count++;
			}
		}

		if (count > 0) match = match / count;

		return match;
	}

	// Blends the features of this entity with the corresponding (by id) features of another entity
	// Where the other entity has features that this one lacks, this entity inherits those features
	// Weight (from 0.0 to 1,0) dictates the proportion of the new feature inherited as a weighted average
	// A weight of 1.0 means the inherited feature replaces the existing one, and 0.0 means there is no effect
	// A weight of 0.5 means the new feature is the balanced average of the two existing features.
	Entity blend(Entity otherEntity, double weight) {
		Feature thisFeature, otherFeature;
		for (String id : otherEntity.features.keySet()) {
			thisFeature = features.get(id);
			otherFeature = otherEntity.getFeature(id);
			if (thisFeature == null) thisFeature = new Feature(otherFeature.getLength(), 0.0);
			thisFeature.blend(otherFeature, weight);
			features.put(id, thisFeature);
		}

		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

}
