package http.server.application;

public class Constants {
    public static final String INSERT_NEW_VISIT_QUERY =
            "insert into db_spec.visit(fio, contact, start_time, end_time) \n" +
                    "values(?, ?, ?, ?)";

    public static final String UPDATE_EXIST_VISIT_QUERY =
            "update db_spec.visit \n" +
                    "set fio = ?, contact = ?, start_time = ?, end_time = ? \n" +
                    "where id = ?";

    public static final String DELETE_VISIT_BY_ID_QUERY =
            "delete from db_spec.visit where id = ?";

    public static final String CHECK_EXIST_VISIT_BY_ID_QUERY =
            "select * from db_spec.visit where id = ?";

    public static final String SELECT_VISIT_BY_ID_QUERY =
            "select fio, contact, start_time, end_time \n" +
                    "from db_spec.visit where id = ?";

    public static final String SELECT_VISIT_BY_OVERLAPS_PERIOD_QUERY =
            "select fio, contact, start_time, end_time \n" +
                    "from db_spec.visit " +
                    "where id != ? and not(start_time >= ? or end_time <= ?)";

    public static final String SELECT_All_VISIT_QUERY =
            "select id, fio, contact, start_time, end_time \n" +
                    "from db_spec.visit order by id asc";
}
