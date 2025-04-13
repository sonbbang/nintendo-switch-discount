package model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import util.NintendoUtil;

@ToString
@Setter
@Getter
public class GameInfo {

	private int index;
	private String title;
	private String image;
	private String players;
	private String genre;
	private Integer price = 0;
	private Integer discountPrice;
	private String desc;
	private Integer discountPercent;
	private String salePeriod;
	private Integer resultPrice;
	private boolean isSupportKorean;
	private String released;
	private String nintendoDetailUrl;
	
	public String toHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr>\n");
		sb.append("<td style=\"width: 20%;\">");
		sb.append("<p><a href=\"").append(this.nintendoDetailUrl)
		   .append("\" target=\"_blank\" rel=\"noopener\">")
		   .append(this.title).append("</a></p>");

		if (this.image != null) {
			sb.append(NintendoUtil.imageTagMax(this.image, "215", "120"));
		}
		sb.append("</td>");

		sb.append("<td>");
		if (this.genre != null) {
			sb.append("<br />\n")
			  .append(this.released)
			  .append("<br />\n")
			  .append(this.genre)
			  .append("<br />\n")
			  .append(this.players);
		}
		sb.append("</td>\n");

		sb.append("<td style=\"width: 20%;\">")
		  .append(this.salePeriod)
		  .append("</td>\n");

		sb.append(this.discountPercent >= 88 ? 
				 "<td style=\"width: 15%;\" bgcolor=\"#FFC5D0\">" : 
				 "<td style=\"width: 15%;\">");

		sb.append("<b><del>")
		  .append(String.format("%,d", this.price))
		  .append("원</del><br />\n")
		  .append(String.format("%,d", this.discountPrice))
		  .append("원</b>(")
		  .append(NintendoUtil.discountPercentColoring(this.discountPercent))
		  .append("%)")
		  .append("</td>\n");

		sb.append("<td style=\"width: 10%;\">")
		  .append(this.isSupportKorean ? "O" : "X")
		  .append("</td>\n");

		sb.append("</tr>\n");
		return sb.toString();
	}

}
