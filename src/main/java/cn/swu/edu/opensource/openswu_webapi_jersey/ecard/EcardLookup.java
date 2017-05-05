package cn.swu.edu.opensource.openswu_webapi_jersey.ecard;

import cn.swu.edu.opensource.openswu_webapi_jersey.constant.Constant;
import cn.swu.edu.opensource.openswu_webapi_jersey.interfaces.Lookup;
import cn.swu.edu.opensource.openswu_webapi_jersey.interfaces.Parameter;
import cn.swu.edu.opensource.openswu_webapi_jersey.utils.Client;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 西南大学开源协会 陈思定  on 2016/8/23.
 * <p>
 * Email : sidingchan@gmail.com
 */
public class EcardLookup implements Lookup {

    private EcardParameter ecardParam;
    private Client client;

    //将个人信息页面解析的结果存放到map中,用gson将map转换为json返回。
    private LinkedHashMap<String, String> resMap;

    //将消费记录页面解析的结果存放到list中,用gson将list转换为json返回。
    private ArrayList<EcardInformation> resList = new ArrayList<>();

    public EcardLookup() {
        client = new Client();
    }

    @Override
    public String lookup(Parameter parameter) {
        ecardParam = (EcardParameter) parameter;

        //Constant.urlEcard = "http://ecard.swu.edu.cn/search/oracle/queryresult.asp?cardno={0}&password={1}"
        String url = MessageFormat.format(Constant.urlEcard, ecardParam.getCardno(), ecardParam.getPassword());

        //如果需要得到个人信息，则解析个人信息页面，若不需要则只get而不解析个人信息页面
        if (ecardParam.getPersonal()) {
            resMap = new LinkedHashMap<>();
            showPersonalPageParse(client.doGet(url));
        } else {
            client.doGet(url);
        }
        // 获取交易记录页数
        int offset = 0;
        Matcher pageMatcher = Pattern.compile("<font color=red>[\\s]*([\\S]*)[\\s]*</font>").matcher(client.doGet(Constant.urlEcardFinance));
        if (pageMatcher.find()) {
            offset = Integer.valueOf(pageMatcher.group(1));
        }
        for (; offset > 0; offset--) {
            String financeUrl = Constant.urlEcardFinance + "?offset=" + offset;
            financePageParse(client.doGet(financeUrl));
        }

        //对消费记录按时间排序
        sortList();

        if (resMap != null) {
            // 需要返回校园卡个人信息,故组合一下json
            return new Gson().toJson(resMap).replace("}", ",\"records\":") + new Gson().toJson(resList) + "}";
        } else {
            return new Gson().toJson(resList);
        }
    }

    private void financePageParse(String htmlFormatText) {
        try {
            String html = new String(htmlFormatText.getBytes("ISO-8859-1"), "gb2312");

            Pattern p1 = Pattern.compile("<TD height=\"26\" align=left>[\\s]*(.*)[\\s]*</TD>");
            Pattern p2 = Pattern.compile("<TD align=left>[\\s]*(.*)[\\s]*</TD>");
            Pattern p3 = Pattern.compile("<TD align=right>[\\s]*(.*)[\\s]*</TD>");
            Pattern p4 = Pattern.compile("<TD align=right>[\\s]*(.*)[\\s]*</TD>");
            Pattern p5 = Pattern.compile("<TD align=right>[\\s]*(.*)[\\s]*</TD>");
            Pattern p6 = Pattern.compile("<TD align=right>[\\s]*(.*)[\\s]*</TD>");
            Pattern p7 = Pattern.compile("<td align=left>[\\s]*(.*)[\\s]*</td>");

            Matcher timeMatcher = p1.matcher(html);
            Matcher typeMatcher = p2.matcher(html);
            Matcher frequencyMatcher = p3.matcher(html);
            Matcher moneyBeforeTradeMatcher = p4.matcher(html);
            Matcher tradeMoneyMatcher = p5.matcher(html);
            Matcher moneyAfterTradeMatcher = p6.matcher(html);
            Matcher placeMatcher = p7.matcher(html);

            while (timeMatcher.find()
                    && typeMatcher.find(timeMatcher.end())
                    && frequencyMatcher.find(typeMatcher.end())
                    && moneyBeforeTradeMatcher.find(frequencyMatcher.end())
                    && tradeMoneyMatcher.find(moneyBeforeTradeMatcher.end())
                    && moneyAfterTradeMatcher.find(tradeMoneyMatcher.end())
                    && placeMatcher.find(moneyAfterTradeMatcher.end())) {

                String time = timeMatcher.group(1);
                String type = typeMatcher.group(1);
                String frequency = frequencyMatcher.group(1);
                String moneyBeforeTrade = moneyBeforeTradeMatcher.group(1);
                String tradeMoney = tradeMoneyMatcher.group(1);
                String moneyAfterTrade = moneyAfterTradeMatcher.group(1);
                String place = placeMatcher.group(1);

                resList.add(new EcardInformation(time, type, frequency, moneyBeforeTrade, tradeMoney, moneyAfterTrade, place));
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void showPersonalPageParse(String htmlFormatText) {

        try {
            String html = new String(htmlFormatText.getBytes("ISO-8859-1"), "gb2312");

            Matcher attMatcher = Pattern
                    .compile("<div align=\"center\">[\\s]*([\\S]*)[\\s]*</div>")
                    .matcher(html);

            Matcher valueMatcher = Pattern
                    .compile("<div align=\"left\">[\\s]*([\\S]*)[\\s]*</div>")
                    .matcher(html);

            String att, value;
            while (attMatcher.find() && valueMatcher.find(attMatcher.end())) {
                att = attMatcher.group(1);
                value = valueMatcher.group(1);

                /*
                * 学校的奇葩html中有很多无用标签,上面valueMatcher.find(attMatcher.end())每次都从找到属性的地方开始找属性值
                * 也是考虑到这些无用却相同的标签
                */
                if (att.equals("") || att == null) continue;

                // switch case 同样不是多余的，att会匹配到一些无用的值，通过switch case匹配需要的值并将其转换为英文属性
                switch (att) {
                    case "姓名":
                        att = "name";
                        break;
                    case "性别":
                        att = "sex";
                        break;
                    case "部门":
                        att = "department";
                        break;
                    case "身份":
                        att = "degree";
                        break;
                    case "证件号码":
                        att = "IDcard";
                        break;
                    case "一卡通号":
                        att = "cardSerialNumber";
                        break;
                    case "显示卡号":
                        att = "cardno";
                        break;
                    case "绑定的银行卡号":
                        att = "bankcard";
                        break;
                    case "借书证号":
                        att = "libraryCard";
                        break;
                    case "卡注册日期":
                        att = "cardRegistrationDate";
                        break;
                    case "卡有效期":
                        att = "cardValidDate";
                        break;
                    case "卡状态":
                        att = "cardStatus";
                        break;
                    default:
                        att = "";
                        break;
                }
                if (!att.equals("")) {
                    resMap.put(att, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    //对消费记录按时间排序
    private void sortList() {
        Collections.sort(resList, (o1, o2) -> {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                if (df.parse(o1.getTime()).getTime() < df.parse(o2.getTime()).getTime()) {
                    return 1;
                } else if (df.parse(o1.getTime()).getTime() > df.parse(o2.getTime()).getTime()) {
                    return -1;
                } else {
                    return 0;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }
}
