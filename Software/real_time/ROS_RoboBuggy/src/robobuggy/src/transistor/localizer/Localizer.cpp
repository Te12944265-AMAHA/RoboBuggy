#include "transistor/localizer/Localizer.h"

void Localizer::Encoder_Callback(const robobuggy::Encoder::ConstPtr &msg)
{

    if (prev_encoder_ticks == -1)
    {
        prev_encoder_ticks = msg->ticks;
    }

    long int current_time = get_current_time_millis();
    long int dt = current_time - prev_encoder_time;

    if (dt < 50)
    {
        return;
    }

    double ticks = msg->ticks;
    double dx = ticks - prev_encoder_ticks;
    dx = dx * 0.61 / 7.0 * 0.3048 * 2;
    double body_speed = dx / (dt / 1000.0);

    prev_encoder_time = current_time;
    prev_encoder_ticks = ticks;
}

void Localizer::GPS_Callback(const robobuggy::GPS::ConstPtr &msg)
{
    geodesy::UTMPoint p;
    p.northing = msg->northing;
    p.easting = msg->easting;
    p.band = 'T';
    p.zone = 17;
    
    double heading_cartesian = 0.0;

    if (prev_position_utm.northing != 0)
    {
        double dx = p.easting - prev_position_utm.easting;
        double dy = p.northing - prev_position_utm.northing;

        heading_cartesian = atan2(dx, dy);
        if (sqrt(dx * dx + dy * dy) < 0.25)
        {
            heading_cartesian = x(3, 0);
        }
    }
    prev_position_utm = p;
}

void Localizer::IMU_Callback(const robobuggy::IMU::ConstPtr &msg)
{
}

void Localizer::Feedback_Callback(const robobuggy::Feedback::ConstPtr &msg)
{
    current_steering_angle = msg->steer_angle;
    current_steering_angle = current_steering_angle / 100;
    current_steering_angle = M_PI / 180 * current_steering_angle;
}

void Localizer::init_R()
{
    R <<
    ;

    std::stringstream s;
    s << R << std::endl;

    ROS_INFO("Initialized R Matrix to : \n%s", s.str().c_str());
}

void Localizer::init_P()
{

    P <<
    ;

    std::stringstream s;
    s << P << std::endl;

    ROS_INFO("Initialized P Matrix to : \n%s", s.str().c_str());
}

void Localizer::init_Q_GPS()
{
    Q_GPS <<
    ;

    std::stringstream s;
    s << Q_GPS << std::endl;

    ROS_INFO("Initialized Q_GPS Matrix to : \n%s", s.str().c_str());
}

void Localizer::init_Q_Encoder()
{
    Q_Encoder <<
    ;

    std::stringstream s;
    s << Q_Encoder << std::endl;

    ROS_INFO("Initialized Q_Encoder matrix to : \n%s", s.str().c_str());
}

void Localizer::init_Q_IMU()
{
    Q_IMU <<
    ;

    std::stringstream s;
    s << Q_IMU << std::endl;

    ROS_INFO("Initialized Q_IMU matrix to : \n%s", s.str().c_str());
}

void Localizer::init_C_GPS()
{
    C_GPS <<
    ;

    std::stringstream s;
    s << C_GPS << std::endl;

    ROS_INFO("Initialized C_GPS Matrix to : \n%s", s.str().c_str());
}

void Localizer::init_C_Encoder()
{
    C_Encoder <<
    ;

    std::stringstream s;
    s << C_Encoder << std::endl;

    ROS_INFO("Initialized C_Encoder Matrix to : \n%s", s.str().c_str());
}

void Localizer::init_C_IMU()
{
    C_IMU <<
    ;
}

void Localizer::init_x()
{
    double init_latitude = 40.442616;
    double init_longitude = -79.943336;

    geographic_msgs::GeoPoint gps_point;
    gps_point.latitude = init_latitude;
    gps_point.longitude = init_longitude;
    geodesy::UTMPoint init_utm(gps_point);

    x <<
      init_utm.northing, // TODO initial lat in UTM
      init_utm.easting, // TODO initial lon in UTM
      0,
      0, // TODO initial heading in rad
      0
    ;

    x_hat <<
          0,
          0,
          0,
          0,
          0
    ;

    std::stringstream s;
    s << x << std::endl;

    ROS_INFO("Initialized x Matrix to : \n%s", s.str().c_str());
}

const std::string Localizer::NODE_NAME = "Localizer";
Localizer::Localizer()
{
    current_steering_angle = 0;
    WHEELBASE_M = 1.13;

    init_R();
    init_P();
    init_Q_GPS();
    init_Q_Encoder();
    init_Q_IMU();
    init_C_GPS();
    init_C_Encoder();
    init_C_IMU();
    init_x();
    update_motion_model(0);

    struct timeval tp;
    gettimeofday(&tp, NULL);
    previous_update_time_ms  = tp.tv_sec * 1000 + tp.tv_usec / 1000;
    prev_encoder_time = previous_update_time_ms;
    prev_encoder_ticks = -1;

    // TODO Work IMU into KF
    imu_sub = nh.subscribe<robobuggy::IMU>("IMU", 1000, &Localizer::IMU_Callback, this);
    gps_sub = nh.subscribe<robobuggy::GPS>("GPS", 1000, &Localizer::GPS_Callback, this);
    enc_sub = nh.subscribe<robobuggy::Encoder>("Encoder", 1000, &Localizer::Encoder_Callback, this);
    steering_sub = nh.subscribe<robobuggy::Feedback>("Feedback", 1000, &Localizer::Feedback_Callback, this);
    pose_pub = nh.advertise<robobuggy::Pose>("Pose", 1000);

}

void Localizer::update_position_estimate()
{
    double easting_x = x_hat(ROW_X, 0);
    double northing_y = x_hat(ROW_Y, 0);
    geodesy::UTMPoint utm_point(easting_x, northing_y, 17, 'T');
    geographic_msgs::GeoPoint gps_point = geodesy::toMsg(utm_point);
    double heading = x_hat(ROW_HEADING, 0);

    robobuggy::Pose p;
    ros::Time time_now = ros::Time::now();
    p.header.stamp = time_now;
    p.heading_rad = heading;
    p.latitude_deg = gps_point.latitude;
    p.longitude_deg = gps_point.longitude;

    pose_pub.publish(p);

}

void Localizer::update_motion_model(double dt)
{

    A <<
      1, 0, dt * cos(x(ROW_HEADING, 0)), 0, 0,
      0, 1, dt * sin(x(ROW_HEADING, 0)), 0, 0,
      0, 0, 1, 0, 0,
      0, 0, 0, 1, dt,
      0, 0, tan(current_steering_angle) / WHEELBASE_M, 0, 0
    ;

}

double Localizer::clamp_angle(double theta)
{
    while (theta < -M_PI)
    {
        theta += 2*M_PI;
    }
    while (theta > M_PI)
    {
        theta -= 2*M_PI;
    }

    return theta;
}

void Localizer::propagate()
{
    long int current_time = get_current_time_millis();
    // convert to seconds
    double dt = (current_time - previous_update_time_ms) / 1000.0;

    update_motion_model(dt);
    x_hat = A * x;
}

long Localizer::get_current_time_millis()
{
    struct timeval tp;
    gettimeofday(&tp, NULL);
    return tp.tv_sec * 1000 + tp.tv_usec / 1000;
}

void Localizer::kalman_filter(MatrixXd c, MatrixXd q, MatrixXd z)
{
}
