package com.galacticpurpose.terraforming;

/**
 * Shared specifications for all four Mars terraforming machines.
 */
public enum TerraformingMachineType {
	THERMAL_HEATER("thermal_heater", 20_000L, 500L, 40L, 8.0, 0.0, 0.0, 0.0f, 0.0f),
	PRESSURE_DRILL("pressure_drill", 25_000L, 600L, 55L, 1.0, 8.0, 0.0, 0.0f, 0.0f),
	FLORA_OXYGENATOR("flora_oxygenator", 20_000L, 500L, 45L, 0.0, 0.5, 8.0, 10.0f, 5.0f),
	ATMOSPHERE_CONDENSER("atmosphere_condenser", 50_000L, 1_200L, 120L, 6.0, 8.0, 10.0, 25.0f, 20.0f);

	private final String id;
	private final long energyCapacity;
	private final long maxInputPerTick;
	private final long energyPerTick;
	private final double heatPerSecond;
	private final double pressurePerSecond;
	private final double oxygenPerSecond;
	private final float minHeatPercent;
	private final float minPressurePercent;

	TerraformingMachineType(
		String id,
		long energyCapacity,
		long maxInputPerTick,
		long energyPerTick,
		double heatPerSecond,
		double pressurePerSecond,
		double oxygenPerSecond,
		float minHeatPercent,
		float minPressurePercent
	) {
		this.id = id;
		this.energyCapacity = energyCapacity;
		this.maxInputPerTick = maxInputPerTick;
		this.energyPerTick = energyPerTick;
		this.heatPerSecond = heatPerSecond;
		this.pressurePerSecond = pressurePerSecond;
		this.oxygenPerSecond = oxygenPerSecond;
		this.minHeatPercent = minHeatPercent;
		this.minPressurePercent = minPressurePercent;
	}

	public String id() {
		return this.id;
	}

	public long energyCapacity() {
		return this.energyCapacity;
	}

	public long maxInputPerTick() {
		return this.maxInputPerTick;
	}

	public long energyPerTick() {
		return this.energyPerTick;
	}

	public double heatPerSecond() {
		return this.heatPerSecond;
	}

	public double pressurePerSecond() {
		return this.pressurePerSecond;
	}

	public double oxygenPerSecond() {
		return this.oxygenPerSecond;
	}

	public float minHeatPercent() {
		return this.minHeatPercent;
	}

	public float minPressurePercent() {
		return this.minPressurePercent;
	}

	public boolean canOperate(MarsProgressSnapshot snapshot) {
		return snapshot.heatPercent() >= this.minHeatPercent
			&& snapshot.pressurePercent() >= this.minPressurePercent;
	}

	public static TerraformingMachineType byOrdinal(int ordinal) {
		TerraformingMachineType[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return THERMAL_HEATER;
		}
		return values[ordinal];
	}

	public static TerraformingMachineType byId(String id) {
		for (TerraformingMachineType type : values()) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return THERMAL_HEATER;
	}
}
