package com.rick.erp.module.common.constant;

import lombok.experimental.UtilityClass;

/**
 * @author Rick.Xu
 * @date 2025/11/23 20:17
 */
@UtilityClass
public class DictValueSQLConstants {

    // GROUP
    public static final String MATERIAL_GROUP_DICT = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'MATERIAL'";

    public static final String SO_LOADING_GROUP = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'SO_LOADING_GROUP'";

    public static final String SALES_ORG = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'SALES_ORG'";

    public static final String DISTRIBUTION_CHANNEL = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'DISTRIBUTION_CHANNEL'";

    public static final String MATERIAL_PRICE_GROUP = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'MATERIAL_PRICE_GROUP'";

    public static final String DIRECT_DISCOVERY_GROUP_DICT = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'DIRECT_DISCOVERY'";

    public static final String PURCHASING_ORG_DICT = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'PURCHASING_ORG'";

//    public static final String PACKAGING_GROUP_DICT = "SELECT code, description label from sys_code_description WHERE code = ? AND category = 'PACKAGING'";

    public static final String DIVISION_GROUP_DICT = "SELECT code, description label from sys_code_description WHERE code = ? AND category = 'DIVISION'";

    // DictValue
    public static final String UNIT_DICT = "select code, description label from mm_unit WHERE code = ? AND (group_id = 0 or group_id = ?)";

    public static final String UNIT_MASS_DICT = "select code, description label from mm_unit WHERE dimension_code = 'MASS' AND code = ? AND (group_id = 0 or group_id = ?)";

    public static final String UNIT_VOLUME_DICT = "select code, description label from mm_unit WHERE dimension_code = 'VOLUME' AND code = ? AND (group_id = 0 or group_id = ?)";

    public static final String PACKAGING_GROUP = "SELECT code, description label from mm_packaging_group WHERE code = :code";

    public static final String PACKAGING_GROUP2 = "SELECT code, description label from mm_packaging_group WHERE code = ?";

    // IdCodeValue
    public static final String PLANT_DICT = "SELECT id, code, description from mm_plant WHERE id = :id";

    public static final String MATERIAL_DICT = "SELECT id, code, description from mm_material WHERE id = :id";

    public static final String PARTNER_DICT = "SELECT id, code, group_name description from mm_partner WHERE id = :id";



}
