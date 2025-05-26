package http.server.application;

public class Constants {
    public static final String SAVE_NEW_VISIT_QUERY =
            "insert into db_spec.appointments(fio, contact, start_time, end_time) \n" +
                    "values(?, ?, ?, ?) returning id";
}
