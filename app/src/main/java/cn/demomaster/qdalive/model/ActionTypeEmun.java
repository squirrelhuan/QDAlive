package cn.demomaster.qdalive.model;

public enum  ActionTypeEmun {

        move(0),//移动
        click(1),//点击
    longClick(2),//长按
    home(5),//桌面
    task(6),//最近任务
    back(7),//返回
    control(10),//控制
    controled(11);//被控制

        private int value = 0;

        ActionTypeEmun(int value) {//必须是private的，否则编译错误
            this.value = value;
        }
        public int value() {
            return this.value;
        }

        public static ActionTypeEmun getEnum(int value) {
            ActionTypeEmun resultEnum = null;
            ActionTypeEmun[] enumArray = ActionTypeEmun.values();
            for (int i = 0; i < enumArray.length; i++) {
                if (enumArray[i].value() == value) {
                    resultEnum = enumArray[i];
                    break;
                }
            }
            return resultEnum;
        }

}
