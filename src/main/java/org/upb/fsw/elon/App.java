package org.upb.fsw.elon;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;


public class App {

	public static void main(String[] args) throws UnableToAnswerException, IOException {

		Elon elon = Elon.getInstance();
		List<IQuestion> testquestions = LoaderController.load(Dataset.QALD8_Test_Multilingual);
		List<IQuestion> trainquestions = LoaderController.load(Dataset.QALD8_Train_Multilingual);
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH_mm"));
		
		try {
			elon.ask(testquestions, "elon_test_" + date + "_" + time + ".json");
			elon.ask(trainquestions, "elon_train_" + date + "_" + time + ".json");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
