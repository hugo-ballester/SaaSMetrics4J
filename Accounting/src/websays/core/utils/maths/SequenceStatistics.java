package websays.core.utils.maths;

import java.util.ArrayList;

public class SequenceStatistics extends ArrayList<Double> {

	public double[] getMinMaxSumAvg() {
		if (size() == 0)
			return new double[] { 0., 0., 0., 0. };
		double min = get(0);
		double max = get(0);
		double sum = 0;

		for (int i = 0; i < size(); i++) {
			if (min > get(i)) {
				min = get(i);
			}
			if (max < get(i)) {
				max = get(i);
			}
			sum += get(i);
		}

		return new double[] { min, max, sum, sum / size() };
	}

	public double max() {
		if (size() == 0)
			return 0;
		double min = get(0);
		for (int i = 0; i < size(); i++) {
			if (min > get(i)) {
				min = get(i);
			}
		}
		return min;
	}

	public double sum() {
		double sum = 0;
		for (int i = 0; i < size(); i++) {
			sum += get(i);
		}
		return sum;
	}
}
