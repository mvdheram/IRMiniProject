import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;
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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class search {
    String tsv = "C:/Users/Meher/Desktop/IR/wiki.tsv";

    public static void main(String args[])
    {
        search search = new search();
        List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);
        try {
            search.index();
            search.measure(questions);
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
            RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
            System.out.println("start indexing");


            String url = null;
            String text = null;
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

   public static Set<String> Query(String text, String answerType)
   {
       Set<String> systemAnswers = new HashSet();
       return null;

   }
    
    public void measure(List<IQuestion> questions) throws Exception
    {

        for (IQuestion question : questions)
        {
            AnswerBasedEvaluation.fMeasure(Query(preprocessing(question.getLanguageToQuestion().get("en")), question.getAnswerType()),question);
        }


    }

}
