package http.server.application;

public class Constants {
    public static final String SAVE_NEW_VISIT_QUERY =
            "insert into db_spec.visit(fio, contact, start_time, end_time) \n" +
                    "values(?, ?, ?, ?) returning id";

    public static final String SAVE_EXIST_VISIT_QUERY =
            "update db_spec.visit \n" +
                    "set fio = ?, contact = ?, start_time = ?, end_time = ? \n" +
                    "where id = ?";

    public static final String VISIT_BY_ID_QUERY =
            "select fio, contact, start_time, end_time \n" +
                    "from db_spec.visit where id = ?";


    public static final String All_VISIT_QUERY =
            "select id, fio, contact, start_time, end_time \n" +
                    "from db_spec.visit order by id asc";
}
