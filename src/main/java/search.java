import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class search {
    static String tsv = "E:\\Misc\\IRMini\\enwiki-20171103-pages.tsv";
    public static RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    public String url="garbage value";
    public String text="garbage value";


    public static void main(String args[])
    {
        search search = new search();
        //List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);
        try {
            //search.index();
            search.Query("When was the Battle of Gettysburg?","date");
            //search.measure(questions);

        }
        catch (Exception e)
        {
            System.out.println("ElasticException:" + e);
        }


    }
    public static String preprocessing(String text) throws Exception
    {
            CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
            Analyzer analyzer = new StandardAnalyzer();
            TokenStream tokenStream = analyzer.tokenStream("contents",new StringReader(text));
            tokenStream = new StopFilter(tokenStream,stopWords);

            StringBuffer sb = new StringBuffer();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken())
            {
                String term = charTermAttribute.toString();
                sb.append(term+" ");

            }

           return sb.toString();

    }

    public void index() throws Exception{
        try {

            System.out.println("start indexing");
            int i = 1;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(tsv))));
            while (br.ready()) {


                String line = br.readLine();

                String[] terms = line.split("\t");

                if (terms.length > 1) {
                    url = line.split("\t")[0];
                    text = line.split("\t")[1];
                }
                IndexRequest indexRequest = new IndexRequest("wiki").id(Integer.toString(i)).source("url", url, "text",  preprocessing(text));
                GetRequest getRequest = new GetRequest("wiki", Integer.toString(i));
                try {
                    GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
                    String sourceAsString = getResponse.getSourceAsString();
                    System.out.println(sourceAsString);
                }
                catch (ElasticsearchException e){
                    if (e.status() == RestStatus.NOT_FOUND)
                    {
                        System.out.println("Index does not exists");
                    }

                }
                i++;
            }
        }

        catch (Exception e) {
            System.out.println("parse exception "+ e);
        }
        System.out.println("done indexing");

    }

   public Set<String> Query(String searchText, String answerType) throws java.io.IOException
   {
       Set<String> systemAnswers = new HashSet();
       SearchRequest searchRequest = new SearchRequest("wiki");
       SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
       QueryBuilder matchQuery = QueryBuilders.matchQuery("text",searchText);
       searchSourceBuilder.query(matchQuery);
       searchRequest.source(searchSourceBuilder);

       SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
       System.out.println("search Responce  for the"+searchText+"is"+ searchResponse.toString());

       JsonElement jsonElement = new JsonParser().parse(searchResponse.toString());
       JsonObject jsonObject = jsonElement.getAsJsonObject();
       jsonObject = jsonObject.getAsJsonObject("hits");
       JsonArray hits_array = jsonObject.getAsJsonArray("hits");
       if(hits_array.size()==0)
       {
           if(answerType.equals("boolean"))
               systemAnswers.add("false");
           else
               systemAnswers.add("no result");
       }
       for(int i=0;i<hits_array.size();i++)
       {
           jsonObject = hits_array.get(i).getAsJsonObject();
           jsonObject= jsonObject.getAsJsonObject("_source");
           if(jsonObject.get("url").toString()!=null)
           {
               url=jsonObject.get("url").toString();
               url=url.replace("/wiki", "/resource");
               url=url.replace("enwikipedia.org", "dbpedia.org");
               url = url.replace("\"", "");
               if(answerType.equals("boolean"))
               {
                   systemAnswers.add("true");
               }
               else if(!answerType.equals("boolean"))
               {
                   systemAnswers.add(url);
               }
           }
           if(jsonObject.get("url").toString()==null)
           {
               systemAnswers.add("no result");
           }

       }

       System.out.println(systemAnswers);

       SearchHits hits = searchResponse.getHits();

       System.out.println("Found " + hits.getTotalHits() + " hits.");

//       SearchHit[] searchHits = hits.getHits();
//       for (SearchHit hit : searchHits) {
//           String sourceAsString = hit.getSourceAsString();
//           float score = hit.getScore();
//           System.out.println("score"+score);
//       }

       return systemAnswers;

   }
    
//    public void measure(List<IQuestion> questions) throws Exception
//    {
//
//        for (IQuestion question : questions)
//        {
//            System.out.println("measure");
//            //AnswerBasedEvaluation.fMeasure(Query(preprocessing(question.getLanguageToQuestion().get("en")), question.getAnswerType()),question);
//            String preprocessd = preprocessing(question.getLanguageToQuestion().get("en"));
//            Query(preprocessd,question.getAnswerType());
//        }
//
//
//    }

}
