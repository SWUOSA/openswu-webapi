package cn.swu.edu.opensource.openswu_webapi_jersey.grade;

import cn.swu.edu.opensource.openswu_webapi_jersey.interfaces.Lookup;
import cn.swu.edu.opensource.openswu_webapi_jersey.utils.Client;

/**
 * Created by csd on 2016/1/30.
 */
public class SwuGrades {

    //    private Client client ;
    private Lookup gradesLookuper;
    // 预留
    private GradeHandler gradeHandler;

    public SwuGrades(Client client) {
//        this.client = client;
        this.gradesLookuper = new GradesLookuper(client);
    }

    public String lookup(SearchParameter searchParam) {

        return this.gradesLookuper.lookup(searchParam);
    }

}
