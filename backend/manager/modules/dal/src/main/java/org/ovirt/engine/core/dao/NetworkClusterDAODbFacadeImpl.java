package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.NetworkClusterId;
import org.ovirt.engine.core.common.businessentities.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.network_cluster;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * <code>NetworkClusterDAODbFacadeImpl</code> provides a concrete implementation of {@link NetworkClusterDAO} based on
 * {@link org.springframework.jdbc.core.JdbcTemplate}
 */
public class NetworkClusterDAODbFacadeImpl extends BaseDAODbFacade implements NetworkClusterDAO {

    private static final ParameterizedRowMapper<network_cluster> mapper =
            new ParameterizedRowMapper<network_cluster>() {
                @Override
                public network_cluster mapRow(ResultSet rs, int rowNum)
                        throws SQLException {
                    network_cluster entity = new network_cluster();
                    entity.setcluster_id(Guid.createGuidFromString(rs.getString("cluster_id")));
                    entity.setnetwork_id(Guid.createGuidFromString(rs.getString("network_id")));
                    entity.setstatus(NetworkStatus.forValue(rs.getInt("status")));
                    entity.setis_display(rs.getBoolean("is_display"));
                    entity.setRequired(rs.getBoolean("required"));
                    return entity;
                }
            };

    @Override
    public network_cluster get(NetworkClusterId id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", id.getClusterId())
                .addValue("network_id", id.getNetworkId());

        return getCallsHandler().executeRead("Getnetwork_clusterBycluster_idAndBynetwork_id", mapper, parameterSource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<network_cluster> getAll() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();

        return getCallsHandler().executeReadList("GetAllFromnetwork_cluster", mapper, parameterSource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<network_cluster> getAllForCluster(Guid clusterid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterid);

        return getCallsHandler().executeReadList("GetAllFromnetwork_clusterByClusterId", mapper,
                parameterSource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<network_cluster> getAllForNetwork(Guid network) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("network_id", network);

        return getCallsHandler().executeReadList("GetAllFromnetwork_clusterByNetworkId",mapper,
                parameterSource);
    }

    @Override
    public void save(network_cluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getcluster_id())
                .addValue("network_id", cluster.getnetwork_id())
                .addValue("status", cluster.getstatus())
                .addValue("is_display", cluster.getis_display())
                .addValue("required", cluster.isRequired());

        getCallsHandler().executeModification("Insertnetwork_cluster", parameterSource);
    }

    @Override
    public void update(network_cluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getcluster_id())
                .addValue("network_id", cluster.getnetwork_id())
                .addValue("status", cluster.getstatus())
                .addValue("is_display", cluster.getis_display())
                .addValue("required", cluster.isRequired());

        getCallsHandler().executeModification("Updatenetwork_cluster", parameterSource);
    }

    @Override
    public void updateStatus(network_cluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getcluster_id())
                .addValue("network_id", cluster.getnetwork_id())
                .addValue("status", cluster.getstatus());

        getCallsHandler().executeModification("Updatenetwork_cluster_status", parameterSource);
    }

    @Override
    public void remove(Guid clusterid, Guid networkid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterid).addValue("network_id",
                        networkid);

        getCallsHandler().executeModification("Deletenetwork_cluster", parameterSource);
    }

    @Override
    public void setNetworkExclusivelyAsDisplay(Guid clusterId, Guid networkId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterId).addValue("network_id", networkId);

        getCallsHandler().executeModification("set_network_exclusively_as_display", parameterSource);
    }
}
