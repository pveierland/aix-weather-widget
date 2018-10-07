package net.veierland.aix.data;

import net.veierland.aix.util.AixLocationInfo;

public interface AixDataSource {

	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime) throws AixDataUpdateException;

}
