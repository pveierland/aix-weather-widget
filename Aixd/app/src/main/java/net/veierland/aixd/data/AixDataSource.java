package net.veierland.aixd.data;

import net.veierland.aixd.util.AixLocationInfo;

public interface AixDataSource {

	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime) throws AixDataUpdateException;

}
