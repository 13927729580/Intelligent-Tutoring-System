package edu.gzhu.its;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.vdurmont.emoji.EmojiParser;

import edu.gzhu.its.base.util.EmojiFilter;
import edu.gzhu.its.corpus.entity.UserComment;
import edu.gzhu.its.corpus.entity.UserTask;
import edu.gzhu.its.corpus.service.IUserCommentService;
import edu.gzhu.its.corpus.service.IUserTaskService;
import edu.gzhu.its.system.entity.User;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserCommentTest {

	@Resource
	private IUserCommentService userCommentService;

	private static final String EXCEL_XLS = "xls";
	private static final String EXCEL_XLSX = "xlsx";

	@Resource
	private IUserTaskService userTaskService;

	/**
	 * 判断Excel的版本,获取Workbook
	 * 
	 * @param in
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Workbook getWorkbok(InputStream in, File file) throws IOException {
		Workbook wb = null;
		if (file.getName().endsWith(EXCEL_XLS)) { // Excel 2003
			wb = new HSSFWorkbook(in);
		} else if (file.getName().endsWith(EXCEL_XLSX)) { // Excel 2007/2010
			wb = new XSSFWorkbook(in);
		}
		return wb;
	}

	/**
	 * 判断文件是否是excel
	 * 
	 * @throws Exception
	 */
	public static void checkExcelVaild(File file) throws Exception {
		if (!file.exists()) {
			throw new Exception("文件不存在");
		}
		if (!(file.isFile() && (file.getName().endsWith(EXCEL_XLS) || file.getName().endsWith(EXCEL_XLSX)))) {
			throw new Exception("文件不是Excel");
		}
	}

	@Test
	@Transactional
	@Rollback(false)
	public void update() throws Exception {
		String[] types = { "economic", "engineering", "history", "literature", "medicine", "science" };
		SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int i = 0; i < types.length; i++) {
			File file = new File("D:\\corpus\\" + types[i]);
			File[] files = file.listFiles();
			for (int j = 0; j < files.length; j++) {
				File excelFile = files[j];
				FileInputStream in = new FileInputStream(excelFile); // 文件流
				checkExcelVaild(excelFile);
				String name = excelFile.getName();
				Workbook workbook = getWorkbok(in, excelFile);
				Sheet sheet = workbook.getSheetAt(0);
				int k = 0;
				Set<String> set = new HashSet<String>();
				for (Row row : sheet) {
					if (k >= 2) {
						String content = EmojiParser.removeAllEmojis(row.getCell(0).toString());
						System.out.println(content + "   " + types[i] + "   " + excelFile);
						if (!set.contains(content)) {
							UserComment comment = new UserComment();

							String createTime = row.getCell(1).toString();
							comment.setCreateTime(sdformat.parse(createTime));

							String against = row.getCell(2).toString();
							comment.setCourse(name.replace(".xls", ""));
							comment.setAgainst(Integer.parseInt(against.replace(".0", "")));
							comment.setContent(content);
							String favCount = row.getCell(3).toString();
							comment.setFavCount(Integer.parseInt(favCount.replace(".0", "")));

							String vote = row.getCell(4).toString();
							comment.setVote(Integer.parseInt(vote.replace(".0", "")));
							comment.setCourseType(types[i]);
							this.userCommentService.save(comment);

						}
						set.add(content);
					}
					k++;

				}
			}

		}

	}

	@Test
	@Transactional
	@Rollback(false)
	public void create() throws Exception {
		List<UserComment> comments = this.userCommentService.findAll();
		for (Iterator iterator = comments.iterator(); iterator.hasNext();) {
			UserComment userComment = (UserComment) iterator.next();
			User user = new User();
			user.setId(1l);
			UserTask task = new UserTask();
			task.setAnnotationed(false);
			task.setUser(user);
			task.setUserComment(userComment);
			this.userTaskService.save(task);
		}
	}

}
