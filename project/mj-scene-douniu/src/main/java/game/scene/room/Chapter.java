package game.scene.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mj.data.poker.douniu.DouniuPai;
import mj.data.poker.douniu.DouniuPaiType;
import mj.data.poker.douniu.DouniuPoker;
import mj.net.message.game.douniu.DNChapterInfo;
import mj.net.message.game.douniu.DNChapterPKResult;
import mj.net.message.game.douniu.DNXiaZhu;
import mj.net.message.game.douniu.SeatUserInfo;
import mj.net.message.game.douniu.UserPkResult;

public class Chapter {
	public static final Logger log = LoggerFactory.getLogger(Chapter.class);
	private UserSeat[] seats;
	private int[] zhus;
	private int zhuangIndex;
	private int currentIndex;
	private List<Integer> qiangZhuangChache = new ArrayList<Integer>();
	Random random = new Random();
	Room room;
	public Chapter(int userNum,Room room) {
		super();
		this.seats = new UserSeat[userNum];
		this.zhus = new int[userNum];
		this.room = room;
	} 
	public UserSeat[] getSeats() {
		return seats;
	}
	public void setSeats(UserSeat[] seats) {
		this.seats = seats;
	}
	public void updateUser(SceneUser user){
		int index = user.getLocationIndex();
		int userid = user.getUserId();
		UserSeat userSeat = seats[index];
		if(userSeat==null){
			userSeat = new UserSeat();
			userSeat.setUserid(userid);
			userSeat.setIndex(index);
			seats[index] = userSeat;
		}else{
			userSeat.setUserid(userid);
			userSeat.setIndex(index);
		}
	}
	public DNChapterInfo toMessage(int index,boolean isKPQZ){
		DNChapterInfo info = new DNChapterInfo();
		info.setCurrentIndex(currentIndex);
		info.setZhuangIndex(zhuangIndex);
		info.setCurrentChapterNum(room.roomInfo.getCurrentChapterNum());
		List<SeatUserInfo> seatsInfos = new ArrayList<SeatUserInfo>();
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null){
				SeatUserInfo seatInfo = userSeat.toMessage(index,isKPQZ);
				seatsInfos.add(seatInfo);
			}
			
		}
		info.setSeats(seatsInfos);
		info.setShowLP(!isKPQZ);
		return info;
		
	}
	public int userNum(){
		int num = 0;
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null){
				++num;
			}
			
		}
		return num;
	}
	

	public int getZhuangIndex() {
		return zhuangIndex;
	}
	
	public void startGame(String str,boolean isKPQZ) {
		try {
			room.getRoomInfo().setBeforeFirstStart(false);
			if("KPQZ".equals(str)){
				if(isKPQZ){
					startClear();
					DNFaPaiUtil.faPai(seats);
				}
			}else{
				startClear();
				DNFaPaiUtil.faPai(seats);
			}
			for (int i = 0; i < seats.length; i++) {
				room.sendMessage(i, this.toMessage(i,isKPQZ));
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
	}
	
	private void startClear() {
		qiangZhuangChache.clear();
		for (int i = 0; i < zhus.length; i++) {
			 zhus[i] = 0;
		}
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null){
				userSeat.setKaiPai(false);
//				userSeat.setZhu(0);
				userSeat.setPai(null);
				userSeat.setPokers(null);
			}
			
		}
		
	}
	public int qiangZhuang(int locationIndex, boolean isall,boolean qiang) {
		if(qiang){
			qiangZhuangChache.add(locationIndex);
		}
		if(isall){
			if(qiangZhuangChache.size()>0){
				int r = random.nextInt(qiangZhuangChache.size());
				this.zhuangIndex = qiangZhuangChache.get(r);
				return r;
			}else{
				int r = random.nextInt(userNum());
				this.zhuangIndex = r;
				return r;
			}
		}
		return -1;
	}
	public boolean kaiPai(SceneUser user) {
		UserSeat userSeat = seats[user.getLocationIndex()];
		if(userSeat.isKaiPai()){
			return false;
		}
		userSeat.setKaiPai(true);
		room.sendMessage(seats[user.getLocationIndex()].toMessage(user.getLocationIndex(),false),null);
		boolean isAll = true;
		for (int i = 0; i < seats.length; i++) {
			UserSeat u = seats[i];
			if(u!=null && !u.isKaiPai()){
				isAll = false;
				break;
			}
		}
		return isAll;
	}
	
	
	public DNChapterPKResult pk() {
		UserSeat zhuang = seats[zhuangIndex];
		DouniuPai pai = zhuang.getPai();
		DNChapterPKResult chapterPKResult = new DNChapterPKResult();
		List<UserPkResult> pkResults = new ArrayList<UserPkResult>();
		int zhuangScore = 0;
//		for (int i = 0; i < seats.length; i++) {
//			UserSeat userSeat = seats[i];
//			if(userSeat!=null){
//				System.out.println(userSeat.getIndex()+"---"+userSeat.getZhu()+"---"+userSeat.getUserid());
//			}
//		}
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null && i!=zhuangIndex){
				UserPkResult result = new UserPkResult();
				DouniuPai upai = userSeat.getPai();
				result.setIndex(userSeat.getIndex());
				result.setPai(DouniuPoker.toIntArrayFromDouniuArray(upai.getPais()));
				result.setPaiType(upai.getType().getIndex());
				result.setZhu(userSeat.getZhu());
				int score = userSeat.getZhu();
				int pk = upai.compareTo(pai);
				if(pk==1){
					 score = upai.getType().getDoubleCount()*score;
					 result.setScore(score);
					 zhuangScore -=  score;
					 result.setPkResult(true);
				}else if(pk==0){
					result.setScore(0);
				}else{
					score = pai.getType().getDoubleCount()*score;
					result.setScore(-score);
					zhuangScore += score;
					result.setPkResult(false);
				}
				pkResults.add(result);
			}//if(userseat!=null)
		}//for
		for (int i = 0; i < seats.length; i++) {
			UserSeat seat = seats[i];
			if(seat!=null){
				seat.setZhu(0);
			}
		}
		chapterPKResult.setPai(DouniuPoker.toIntArrayFromDouniuArray(pai.getPais()));
		chapterPKResult.setPaiType(pai.getType().getIndex());
		chapterPKResult.setUserResults(pkResults);
		chapterPKResult.setZhuangIndex(zhuangIndex);
		chapterPKResult.setSocre(zhuangScore);
		return chapterPKResult;
//		room.sendMessage(chapterPKResult,null);
		
	}
	public void changeZhuang(int nextInt) {
		this.zhuangIndex = nextInt;
	}
	public boolean xiazhu(DNXiaZhu msg, SceneUser user) {
		int zhuang = getZhuangIndex();
//		seats[user.getLocationIndex()].setZhu(msg.getZhu());
//		for (int i = 0; i < seats.length; i++) {
//			UserSeat userSeat = seats[i];
//			if(userSeat!=null){
//				System.out.println(userSeat.getIndex()+"---"+userSeat.getZhu());
//			}
//		}
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
//			if(userSeat!=null){
//				System.out.println(userSeat.getZhu()+"---"+zhuangIndex+"---"+userSeat.getIndex());
//			}
			if(userSeat!=null && userSeat.getZhu()<=0 && zhuangIndex!=userSeat.getIndex()){
				return false;
			}
		}
		return true;
		
	}
	public void exitUser(int userId) {
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null && userSeat.getUserid() == userId){
				seats[i] = null;
			}
			
		}
		
	}
	public void saveZhu(int zhu, int locationIndex) {
		seats[locationIndex].setZhu(zhu);
//		for (int i = 0; i < seats.length; i++) {
//			UserSeat userSeat = seats[i];
//			if(userSeat!=null){
//				System.out.println(userSeat.getIndex()+"---"+userSeat.getZhu()+"---"+userSeat.getUserid());
//			}
//		}
	}
	public int[] getZhus() {
		return zhus;
	}
	public void setZhus(int[] zhus) {
		this.zhus = zhus;
	}
	public void clearSeats() {
		for (int i = 0; i < seats.length; i++) {
			UserSeat userSeat = seats[i];
			if(userSeat!=null){
				userSeat.setZhu(0);
			}
			
		}
	}
	
}
