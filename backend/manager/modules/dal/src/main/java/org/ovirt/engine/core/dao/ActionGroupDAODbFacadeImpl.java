package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.action_version_map;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * <code>ActionGroupDAODbFacadeImpl</code> provides a concrete implementation of {@link ActionGroupDAO}.
 *
 * The initial implementation came from  {@link org.ovirt.engine.core.dal.dbbroker.DbFacade}.
 */
public class ActionGroupDAODbFacadeImpl extends BaseDAODbFacade implements ActionGroupDAO {
    @Override
    public List<ActionGroup> getAllForRole(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);

        return getCallsHandler().executeReadList("GetRoleActionGroupsByRoleId",
                ActionGroupMapper.instance,
                parameterSource);
    }

    @Override
    public action_version_map getActionVersionMapByActionType(VdcActionType action_type) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("action_type", action_type);

        return getCallsHandler().executeRead("Getaction_version_mapByaction_type",
                ActionVersionMapMapper.instance,
                parameterSource);
    }

    @Override
    public void addActionVersionMap(action_version_map action_version_map) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("action_type",
                action_version_map.getaction_type()).addValue("cluster_minimal_version",
                action_version_map.getcluster_minimal_version()).addValue("storage_pool_minimal_version",
                action_version_map.getstorage_pool_minimal_version());

        getCallsHandler().executeModification("Insertaction_version_map", parameterSource);
    }

    @Override
    public void removeActionVersionMap(VdcActionType action_type) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("action_type", action_type);
        getCallsHandler().executeModification("Deleteaction_version_map", parameterSource);
    }

    @Override
    public List<action_version_map> getAllActionVersionMap() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();
        return getCallsHandler().executeReadList("GetAllFromaction_version_map",
                ActionVersionMapMapper.instance,
                parameterSource);

    }

    private static class ActionVersionMapMapper implements ParameterizedRowMapper<action_version_map> {
        public static final ActionVersionMapMapper instance = new ActionVersionMapMapper();

        @Override
        public action_version_map mapRow(ResultSet rs, int rowNum) throws SQLException {
            action_version_map entity = new action_version_map();
            entity.setaction_type(VdcActionType.forValue(rs.getInt("action_type")));
            entity.setcluster_minimal_version(rs.getString("cluster_minimal_version"));
            entity.setstorage_pool_minimal_version(rs.getString("storage_pool_minimal_version"));
            return entity;
        }
    }

    private static class ActionGroupMapper implements ParameterizedRowMapper<ActionGroup> {
        public static final ActionGroupMapper instance = new ActionGroupMapper();

        @Override
        public ActionGroup mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return ActionGroup.forValue(rs.getInt("action_group_id"));
        }
    }
}
